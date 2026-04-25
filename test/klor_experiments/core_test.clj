(ns klor-experiments.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ring.mock.request :as mock]
            [klor.simulator :refer [simulate-chor]]
            [klor-experiments.storage :as storage]
            [klor-experiments.choreography :refer [list-users create-user update-user delete-user]]
            [klor-experiments.core :refer [app run-chor]]))

;;; Fixtures

(defn reset-storage [test-fn]
  (reset! storage/user-store {})
  (reset! storage/next-id 0)
  (test-fn))

(use-fixtures :each reset-storage)

;;; Storage unit tests

(deftest storage-create
  (let [user (storage/create! {:name "Alice" :email "alice@example.com"})]
    (is (= 1 (:id user)))
    (is (= "Alice" (:name user)))
    (is (= "alice@example.com" (:email user)))))

(deftest storage-all-users
  (storage/create! {:name "Alice" :email "alice@example.com"})
  (storage/create! {:name "Bob" :email "bob@example.com"})
  (is (= 2 (count (storage/all-users)))))

(deftest storage-update
  (let [user    (storage/create! {:name "Alice" :email "alice@example.com"})
        updated (storage/update! (:id user) {:name "Alicia"})]
    (is (= "Alicia" (:name updated)))
    (is (= "alice@example.com" (:email updated)))))

(deftest storage-delete
  (let [user (storage/create! {:name "Alice" :email "alice@example.com"})]
    (storage/delete! (:id user))
    (is (nil? (storage/get-user (:id user))))
    (is (empty? (storage/all-users)))))

;;; Choreography tests (via simulate-chor)

(deftest chor-list-users-empty
  (is (empty? (run-chor list-users))))

(deftest chor-create-user
  (let [user (run-chor create-user {:name "Alice" :email "alice@example.com"})]
    (is (= 1 (:id user)))
    (is (= "Alice" (:name user)))
    (is (= 1 (count (storage/all-users))))))

(deftest chor-list-users-after-create
  (run-chor create-user {:name "Alice" :email "alice@example.com"})
  (run-chor create-user {:name "Bob" :email "bob@example.com"})
  (is (= 2 (count (run-chor list-users)))))

(deftest chor-update-user
  (let [created (run-chor create-user {:name "Alice" :email "alice@example.com"})
        updated (run-chor update-user (:id created) {:name "Alicia" :email "alicia@example.com"})]
    (is (= "Alicia" (:name updated)))
    (is (= "alicia@example.com" (:email updated)))))

(deftest chor-delete-user
  (let [user (run-chor create-user {:name "Alice" :email "alice@example.com"})]
    (run-chor delete-user (:id user))
    (is (empty? (storage/all-users)))))

;;; HTTP handler tests (via ring-mock)

(deftest http-get-root-empty
  (let [resp (app (mock/request :get "/"))]
    (is (= 200 (:status resp)))
    (is (re-find #"Users" (:body resp)))))

(deftest http-create-user
  (let [resp (app (-> (mock/request :post "/users")
                      (mock/content-type "application/x-www-form-urlencoded")
                      (mock/body "name=Alice&email=alice%40example.com")))]
    (is (= 303 (:status resp)))
    (is (= "/" (get-in resp [:headers "Location"])))
    (is (= 1 (count (storage/all-users))))))

(deftest http-list-after-create
  (app (-> (mock/request :post "/users")
           (mock/content-type "application/x-www-form-urlencoded")
           (mock/body "name=Alice&email=alice%40example.com")))
  (let [resp (app (mock/request :get "/"))]
    (is (re-find #"Alice" (:body resp)))))

(deftest http-edit-form
  (let [user (storage/create! {:name "Alice" :email "alice@example.com"})
        resp (app (mock/request :get (str "/users/" (:id user) "/edit")))]
    (is (= 200 (:status resp)))
    (is (re-find #"Alice" (:body resp)))))

(deftest http-update-user
  (let [user (storage/create! {:name "Alice" :email "alice@example.com"})
        resp (app (-> (mock/request :post (str "/users/" (:id user)))
                      (mock/content-type "application/x-www-form-urlencoded")
                      (mock/body "name=Alicia&email=alicia%40example.com")))]
    (is (= 303 (:status resp)))
    (is (= "Alicia" (:name (storage/get-user (:id user)))))))

(deftest http-delete-user
  (let [user (storage/create! {:name "Alice" :email "alice@example.com"})
        resp (app (mock/request :post (str "/users/" (:id user) "/delete")))]
    (is (= 303 (:status resp)))
    (is (nil? (storage/get-user (:id user))))))
