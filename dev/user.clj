(ns user
  (:require [clojure.java.browse :refer [browse-url]]
            [klor-experiments.core :refer [app main]]
            [ring.middleware.refresh :refer [wrap-refresh]]
            [ring.middleware.reload :refer [wrap-reload]]))


(def dev-handler
  (-> #'app
      wrap-reload
      wrap-refresh))


(defonce _
  (let [server (main #'dev-handler {:join? false :port 0})
        port   (.getLocalPort (first (.getConnectors server)))]
    (browse-url (str "http://localhost:" port))))
