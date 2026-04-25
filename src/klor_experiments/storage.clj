(ns klor-experiments.storage)

(def user-store (atom {})) ; {id -> {:id id :name name :email email}}
(def next-id (atom 0))

(defn all-users [] (vals @user-store))

(defn create! [{:keys [name email]}]
  (let [id   (swap! next-id inc)
        user {:id id :name name :email email}]
    (swap! user-store assoc id user)
    user))

(defn update! [id data]
  (swap! user-store update id merge (select-keys data [:name :email]))
  (get @user-store id))

(defn delete! [id]
  (swap! user-store dissoc id)
  id)

(defn get-user [id] (get @user-store id))
