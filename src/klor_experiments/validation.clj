(ns klor-experiments.validation
  (:require [clojure.string :as str]))

(defn validate [{:keys [name email]}]
  (cond
    (str/blank? name)                   "Name cannot be blank"
    (> (count name) 1000)               "Name must be 1000 characters or fewer"
    (str/blank? email)                  "Email cannot be blank"
    (> (count email) 1000)              "Email must be 1000 characters or fewer"
    (not (str/includes? email "@"))     "Email must contain @"
    :else nil))
