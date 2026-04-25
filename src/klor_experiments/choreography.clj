(ns klor-experiments.choreography
  (:require [klor.core :refer :all]
            [klor.simulator :refer [simulate-chor]]
            [klor-experiments.storage :as storage]))

;; List all users: Server reads atom, moves to Client (Client-only result)
(defchor list-users [Client Server]
  (-> #{Client})
  []
  (Server->Client (Server (storage/all-users))))

;; Create user: Client moves form data to Server, Server stores + moves new user to Client
(defchor create-user [Client Server]
  (-> #{Client} #{Client})
  [user-data]
  (let [sdata (Client->Server user-data)]
    (Server->Client (Server (storage/create! sdata)))))

;; Update user: Client moves id + data to Server, Server updates + moves result to Client
(defchor update-user [Client Server]
  (-> #{Client} #{Client} #{Client})
  [user-id user-data]
  (let [sid   (Client->Server user-id)
        sdata (Client->Server user-data)]
    (Server->Client (Server (storage/update! sid sdata)))))

;; Delete user: Client moves id to Server, Server deletes + moves confirmation to Client
(defchor delete-user [Client Server]
  (-> #{Client} #{Client})
  [user-id]
  (let [sid (Client->Server user-id)]
    (Server->Client (Server (storage/delete! sid)))))
