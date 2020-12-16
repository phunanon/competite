(ns competite.db.account
  (:require
    [competite.db.db :refer [db]]
    [pandect.algo.sha256 :refer [sha256]]))

(defn- hash-pass&salt [plain-pass salt-hash]
  (sha256 (str plain-pass salt-hash)))

(defn- pass-ok? [plain-pass pass-hash salt-hash]
  (= (hash-pass&salt plain-pass salt-hash)
     pass-hash))

(defn- rand-pass&salt! [plain-pass]
  (let [salt-hash (sha256 (str (.getTime (java.util.Date.))))
        pass-hash (hash-pass&salt plain-pass salt-hash)]
    {:pass pass-hash :salt salt-hash}))

(defn search-user [by val]
  (->>
    @db
    :users
    vals
    (filter #(= (by %) val))
    first))

(defn email+pass->user [email plain-pass]
  (let [{:keys [id pass salt] :as user}
          (search-user :email email)]
    (if-not id
      :bad-email
      (if-not (pass-ok? plain-pass pass salt)
        :bad-pass
        user))))

(defn new-user! [email plain-pass]
  (if (search-user :email email)
    :bad-email
    (swap! db
      (fn [{:keys [next-user-id] :as db}]
        (-> db
          (update :users conj
            (into
              {:id    next-user-id
               :email email 
               :name "Anonymous"}
              (rand-pass&salt! plain-pass)))
          (assoc :next-user-id (inc next-user-id)))))))