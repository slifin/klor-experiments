(ns klor-experiments.choreography
  (:require [klor.core :refer :all]
            [klor-experiments.storage :as storage]
            [klor-experiments.validation :refer [validate]]))

;; List all users: Server reads atom, moves to Client
(defchor list-users [Client Server]
  (-> #{Client})
  []
  (Server->Client (Server (storage/all-users))))

;; Create user: Client moves form data to Server, Server validates then stores or returns error
(defchor create-user [Client Server]
  (-> #{Client} #{Client})
  [user-data]
  (let [sdata  (Client->Server user-data)
        error  (Server (validate sdata))
        valid? (Server=>Client (Server (nil? error)))]
    (if valid?
      (Server->Client (Server (storage/create! sdata)))
      (Server->Client error))))

;; Update user: Client moves id + data to Server, Server validates then updates or returns error
(defchor update-user [Client Server]
  (-> #{Client} #{Client} #{Client})
  [user-id user-data]
  (let [sid    (Client->Server user-id)
        sdata  (Client->Server user-data)
        error  (Server (validate sdata))
        valid? (Server=>Client (Server (nil? error)))]
    (if valid?
      (Server->Client (Server (storage/update! sid sdata)))
      (Server->Client error))))

;; Delete user: Client moves id to Server, Server deletes + moves confirmation to Client
(defchor delete-user [Client Server]
  (-> #{Client} #{Client})
  [user-id]
  (let [sid (Client->Server user-id)]
    (Server->Client (Server (storage/delete! sid)))))
