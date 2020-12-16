(ns competite.db.polling
  (:require
    [competite.db.db :refer [db]]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn deep-merge [a b]
  (if (map? a) (merge-with deep-merge a b) b))

(defn merge-schema [schema]
  (reset! db
    (assoc @db :comps
      (deep-merge (:comps @db) schema))))

(defn merge-event [comp-id topic payload]
  (reset! db
    (assoc-in @db [:comps comp-id :events topic] payload)))

(defn records-from [comp-id record-key num]
  (println record-key num)
  (into {}
    (drop num
      (sort-by first
        (get-in @db [:comps comp-id record-key])))))

(defn model-token []
  *anti-forgery-token*)