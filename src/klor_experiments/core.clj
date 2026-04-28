(ns klor-experiments.core
  (:require [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.session :refer [wrap-session]]
            [hiccup2.core :as h]
            [clojure.core.async :as a]
            [klor.simulator :refer [simulate-chor]]
            [klor-experiments.choreography :refer [list-users create-user update-user delete-user]]
            [klor-experiments.multi-step :as multi-step]
            [klor-experiments.storage :as storage]
            [klor-experiments.html :as html]))

(defn html-response [hiccup]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str (h/html hiccup))})

(defn redirect [location]
  {:status 303
   :headers {"Location" location}})

(defn redirect-with-error [location error]
  (assoc (redirect location) :flash {:error error}))

(defn run-chor [chor & args]
  (get @(apply simulate-chor chor args) 'Client))

(def app*
  (ring/ring-handler
   (ring/router
    [["/" {:get (fn [req]
                  (html-response (html/users-page (run-chor list-users)
                                                  (get-in req [:flash :error]))))}]

     ["/users" {:post (fn [req]
                        (let [params (get req :params)
                              data   {:name  (get params "name")
                                      :email (get params "email")}
                              result (run-chor create-user data)]
                          (if (string? result)
                            (redirect-with-error "/" result)
                            (redirect "/"))))}]

     ["/users/:id/edit" {:get (fn [req]
                                (let [id (Long/parseLong (get-in req [:path-params :id]))]
                                  (if-let [u (storage/get-user id)]
                                    (html-response (html/edit-page u (get-in req [:flash :error])))
                                    (redirect "/"))))}]

     ["/users/:id" {:post (fn [req]
                             (let [id     (Long/parseLong (get-in req [:path-params :id]))
                                   params (get req :params)
                                   data   {:name  (get params "name")
                                           :email (get params "email")}
                                   result (run-chor update-user id data)]
                               (if (string? result)
                                 (redirect-with-error (str "/users/" id "/edit") result)
                                 (redirect "/"))))}]

     ["/users/:id/delete" {:post (fn [req]
                                   (let [id (Long/parseLong (get-in req [:path-params :id]))]
                                     (run-chor delete-user id)
                                     (redirect "/")))}]

     ["/register"
      {:get  (fn [_]
               (let [sid    (str (java.util.UUID/randomUUID))
                     in-ch  (a/chan 1)
                     out-ch (a/chan 1)]
                 (swap! multi-step/reg-sessions assoc sid {:in-ch in-ch :out-ch out-ch})
                 (future
                   @(simulate-chor multi-step/register-wizard sid)
                   (swap! multi-step/reg-sessions dissoc sid))
                 (html-response (a/<!! out-ch))))
       :post (fn [req]
               (let [sid   (get-in req [:params "session-id"])
                     value (get-in req [:params "value"])]
                 (if-let [session (get @multi-step/reg-sessions sid)]
                   (do
                     (a/>!! (:in-ch session) value)
                     (html-response (a/<!! (:out-ch session))))
                   (redirect "/register"))))}]])

   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))))

(def app (-> app* wrap-params wrap-flash wrap-session))


(defn main [ring-handler opts]
  (System/setProperty "org.slf4j.simpleLogger.log.org.eclipse.jetty" "warn")
  (jetty/run-jetty ring-handler opts))


(defn -main [& _]
  (main #'app {:port 3000}))
