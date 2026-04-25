(ns klor-experiments.core
  (:require [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [hiccup2.core :as h]
            [klor.simulator :refer [simulate-chor]]
            [klor-experiments.choreography :refer [list-users create-user update-user delete-user]]
            [klor-experiments.storage :as storage]
            [klor-experiments.html :as html]))

(defn html-response [hiccup]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str (h/html hiccup))})

(defn redirect [location]
  {:status 303
   :headers {"Location" location}})

(defn run-chor [chor & args]
  (get @(apply simulate-chor chor args) 'Client))

(def app*
  (ring/ring-handler
   (ring/router
    [["/" {:get (fn [_]
                  (html-response (html/users-page (run-chor list-users))))}]

     ["/users" {:post (fn [req]
                        (let [params (get req :params)
                              data   {:name  (get params "name")
                                      :email (get params "email")}]
                          (run-chor create-user data)
                          (redirect "/")))}]

     ["/users/:id/edit" {:get (fn [req]
                                (let [id (Long/parseLong (get-in req [:path-params :id]))]
                                  (if-let [u (storage/get-user id)]
                                    (html-response (html/edit-page u))
                                    (redirect "/"))))}]

     ["/users/:id" {:post (fn [req]
                             (let [id     (Long/parseLong (get-in req [:path-params :id]))
                                   params (get req :params)
                                   data   {:name  (get params "name")
                                           :email (get params "email")}]
                               (run-chor update-user id data)
                               (redirect "/")))}]

     ["/users/:id/delete" {:post (fn [req]
                                   (let [id (Long/parseLong (get-in req [:path-params :id]))]
                                     (run-chor delete-user id)
                                     (redirect "/")))}]])

   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))))

(def app (wrap-params app*))


(defn main [ring-handler opts]
  (System/setProperty "org.slf4j.simpleLogger.log.org.eclipse.jetty" "warn")
  (jetty/run-jetty ring-handler opts))


(defn -main [& _]
  (main #'app {:port 3000}))
