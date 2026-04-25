(ns klor-experiments.html
  (:require [hiccup2.core :as h]))

(defn layout [& body]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Klor Users"]
    [:style
     "body { font-family: monospace; max-width: 700px; margin: 2rem auto; padding: 0 1rem; background: #1a1a1a; color: #e0e0e0; }
      h1, h2 { color: #ccc; }
      table { width: 100%; border-collapse: collapse; margin-bottom: 2rem; }
      th, td { text-align: left; padding: 0.5rem; border-bottom: 1px solid #333; }
      th { color: #888; font-size: 0.85em; text-transform: uppercase; }
      input { background: #2a2a2a; color: #e0e0e0; border: 1px solid #444; padding: 0.4rem 0.6rem; margin-right: 0.5rem; }
      button, .btn { background: #333; color: #e0e0e0; border: 1px solid #555; padding: 0.4rem 0.8rem; cursor: pointer; text-decoration: none; }
      button:hover, .btn:hover { background: #444; }
      .btn-danger { border-color: #844; color: #f88; }
      form { display: inline; }
      .create-form { margin-bottom: 2rem; padding: 1rem; border: 1px solid #333; }"]]
   [:body body]])

(defn users-page [users]
  (layout
   [:h1 "Users"]
   [:div.create-form
    [:h2 "Create User"]
    [:form {:method "post" :action "/users"}
     [:input {:type "text" :name "name" :placeholder "Name" :required true}]
     [:input {:type "email" :name "email" :placeholder "Email" :required true}]
     [:button {:type "submit"} "Create"]]]
   [:table
    [:thead [:tr [:th "ID"] [:th "Name"] [:th "Email"] [:th "Actions"]]]
    [:tbody
     (for [user (sort-by :id users)]
       [:tr
        [:td (:id user)]
        [:td (:name user)]
        [:td (:email user)]
        [:td
         [:a.btn {:href (str "/users/" (:id user) "/edit")} "Edit"]
         " "
         [:form {:method "post" :action (str "/users/" (:id user) "/delete")}
          [:button.btn.btn-danger {:type "submit"} "Delete"]]]])]]))

(defn edit-page [user]
  (layout
   [:h1 "Edit User"]
   [:form {:method "post" :action (str "/users/" (:id user))}
    [:div [:input {:type "text" :name "name" :value (:name user) :required true}]]
    [:br]
    [:div [:input {:type "email" :name "email" :value (:email user) :required true}]]
    [:br]
    [:button {:type "submit"} "Save"]
    " "
    [:a.btn {:href "/"} "Cancel"]]))
