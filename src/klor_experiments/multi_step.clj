(ns klor-experiments.multi-step
  (:require [klor.core :refer :all]
            [clojure.core.async :as a]
            [clojure.string :as str]
            [klor-experiments.html :as html]
            [klor-experiments.storage :as storage]
            [klor-experiments.validation :refer [validate-name validate-email]]))

;; Per-session channel registry: {session-id {:in-ch ch :out-ch ch}}
(def reg-sessions (atom {}))

;; Client-side I/O helpers — only ever called in Client's projection
(defn client-recv [sid]
  (a/<!! (get-in @reg-sessions [sid :in-ch])))

(defn client-send! [sid page]
  (a/>!! (get-in @reg-sessions [sid :out-ch]) page))

;; Multi-step registration wizard as a single persistent choreography.
;;
;; A single simulate-chor instance runs across 3 HTTP requests:
;;   GET /register    → sends name form via out-ch
;;   POST /register   → sends name via in-ch  → choreography stores it and sends email form
;;   POST /register   → sends email via in-ch → choreography validates both, creates user
;;
;; The key: `sname` (the validated name) lives on Server's running thread,
;; not in the HTTP session. It's held in the choreography's lexical scope across
;; the in-ch/out-ch round-trip. No session atom, no cookie — just klor.
(defchor register-wizard [Client Server]
  (-> #{Client} #{Client})
  [session-id]
  ;; Step 1: show name form, collect name
  (let [_      (Client (client-send! session-id (html/register-name-step session-id nil)))
        name   (Client (client-recv session-id))
        sname  (Client->Server name)
        ;; Step 2: show email form, collect email
        _      (Client (client-send! session-id (html/register-email-step session-id nil)))
        email  (Client (client-recv session-id))
        semail (Client->Server email)
        ;; Validate both on Server
        nerr   (Server (validate-name sname))
        eerr   (Server (validate-email semail))
        ok?    (Server=>Client (Server (and (nil? nerr) (nil? eerr))))]
    (if ok?
      ;; Both valid: create user, send success page
      (let [user (Server->Client (Server (storage/create! {:name sname :email semail})))
            _    (Client (client-send! session-id (html/register-success user)))]
        user)
      ;; Validation failed: send error summary, return error string
      (let [errs (Server->Client (Server (str/join "; " (filter some? [nerr eerr]))))
            _    (Client (client-send! session-id (html/register-error errs)))]
        errs))))
