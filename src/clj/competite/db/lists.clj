(ns competite.db.lists
  (:require
    [competite.db.db :refer [db]]
    [medley.core :refer
      [map-vals]]))

(defn for-user
  [{referee? :referees inputs :inputs :as comp} user-id]
  (let [safe (select-keys comp [:title :tags :desc :views :events])]
    (if user-id
      (let [user-id    (bigint user-id)
            inputs     (if (referee? user-id) inputs)]
        (into safe {:inputs inputs :user-id user-id}))
      safe)))

(defn all-comps [& [user-id]]
  (->> @db
    :comps
    (map-vals #(for-user % user-id))
    (into {})))
