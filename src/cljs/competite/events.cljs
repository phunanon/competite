(ns competite.events
  (:require
    [clojure.string :refer [ends-with? split]]
    [re-frame.core :as rf]
    [ajax.edn :as edn]
    [competite.common :refer [get-form-data]]
    [medley.core :refer
      [filter-keys map-keys map-vals map-kv map-kv-vals dissoc-in]]))

; Utils

(defn deep-merge [a b]
  (if (map? a) (merge-with deep-merge a b) b))

(defn topics-match? [& [criteria topic :as topics]]
  "Accepts a subscription criteria and topic,
   and returns boolean upon successful match"
  (let [split      (map #(split % #"/") topics)   ;[["+" "Category"] ["MsgType" "Category"]]
        transposed (apply (partial map vector) split)] ;[["+" "MsgType"] ["Category" "Category"]]
    (if (and (not (ends-with? criteria "#"))
             (apply not= (map count split)))
      false ;The topic was longer than the criteria
      (every? (fn [[cf tf]] (#{"#" "+" tf} cf)) transposed)))) ;All pairs equal, or criteria is "+" or "#"?

(defn- some-topics-match? [subs topic]
  (some #(topics-match? % topic) subs))

;;dispatchers

(rf/reg-event-db :navigate
  (fn [db [_ route]]
    (assoc db :route route)))

(rf/reg-event-db :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-db :dissoc
  (fn [db [_ & ks]]
    (if (< (count ks) 1)
      (update-in db (butlast ks) dissoc (last ks))
      (dissoc db (first ks)))))

(rf/reg-event-db :db
  (fn [db [_ & keys-val]]
    (assoc-in db (butlast keys-val) (last keys-val))))

(rf/reg-event-db :into-db
  (fn [db [_ hmap]]
    (into db hmap)))

; -- HTTP stuff --

(rf/reg-event-fx :http-ok
  (fn [{db :db} [_ callback result]]
    (let [db (deep-merge db result)
          db (assoc db :http-wait false)
          fx {:db db}
          fx (if callback (assoc fx :dispatch [callback result]) fx)]
    fx)))

(rf/reg-event-db :http-bad
  (fn [db _]
    (js/setTimeout #(rf/dispatch [:dissoc :http-bad]) 2000)
    (into db {:http-bad true :http-wait false})))

(rf/reg-event-fx :http
  (fn [{:keys [db]} [_ & [uri params callback]]]
    {:db (assoc db :http-wait true)
     :http-xhrio
       {:method          "post"
        :uri             uri
        :on-success      [:http-ok callback]
        :on-failure      [:http-bad]
        :params          (assoc params :__anti-forgery-token js/csrfToken)
        ;:headers         {"X-CSRF-Token" js/csrfToken}
        :format          (edn/edn-request-format)
        :response-format (edn/edn-response-format)}}))

; -- signin --

(rf/reg-event-db :after-signin
  (fn [db _]
    (if (:user db)
      (dissoc db :signin)
      db)))

(rf/reg-event-fx :sign-or-reg
  (fn [_ [_ endpoint form]]
    {:dispatch [:http endpoint form :after-signin]}))

(rf/reg-event-db :after-signin
  (fn [db _]
    (if (:user db)
      (dissoc db :signin)
      db)))

(rf/reg-event-fx :signout
  (fn [{db :db} _]
    {:db       (dissoc db :user)
     :dispatch-n [[:http "/signout" nil]
                  [:dissoc :user]]}))

(rf/reg-event-fx :mqtt
  (fn [_ [_ comp-id topic payload]]
    {:dispatch
      [:http "/mqtt"
        {:comp-id comp-id
         :mqtt [(str (.getTime (js/Date.)) topic) payload]}]}))


(rf/reg-event-db :merge-polled
  (fn [db [_ {c :comp-id :as to-merge}]]
(print to-merge)
    (assoc-in db [:comps c]
      (deep-merge
        (get-in db [:comps c])
        to-merge))))

(rf/reg-event-fx :poll-server
  (fn [db]
    (let [{c :comp-id e :events i :inputs} @(rf/subscribe [:comp])]
      (when c
        {:dispatch
          [:http "/poll"
            {:comp-id c
             :num-event (count e)
             :give-inputs (seq i)} :merge-polled]}))))

; -- competition --

(rf/reg-event-db :select-comp
  (fn [db [_ comp-id]]
    (assoc db :comp-id (js/parseInt comp-id))))


;;subscriptions

(rf/reg-sub :route
  (fn [db _] (:route db)))

(rf/reg-sub :page
  :<- [:route]
  (fn [route _] (-> route :data :name)))

(rf/reg-sub :common/error
  (fn [db _] (:common/error db)))

(rf/reg-sub :db
  (fn [db [_ & keys]]
    (get-in db keys)))

(rf/reg-sub :db-map
  (fn [db [_ & keys]]
    (or (get-in db keys) {})))

; -- competite subscriptions

(rf/reg-sub :comp
  (fn [{c :comp-id comps :comps} _]
    (if c (into (get comps c) {:comp-id c}) nil)))

(rf/reg-sub :subbed?
  (fn [{subs :subs} [_ criteria]]
    (some (fn [[k]] (#{criteria} k)) subs)))

(rf/reg-event-db :unsub
  (fn [db [_ criteria]]
    (update-in db [:subs] dissoc criteria)))

(rf/reg-event-db :sub
  (fn [db [_ criteria header]]
    (update-in db [:subs] assoc criteria header)))

; -- lists --

(rf/reg-sub :notis
  (fn [_] [(rf/subscribe [:db]) (rf/subscribe [:comp])])
  (fn [[{subs :subs} {:keys [title events]}] [_ & [top-n]]]
    (let [subbed?  #(some-topics-match? (keys subs) %)
          get-time #(js/parseInt (second (re-find #"(.+?)/" %)))]
      (->> events
        (filter-keys subbed?)
        (map (fn [[k {t :text}]] [(get-time k) t]))
        (sort-by first >)
        (take (or top-n (count events)))))))

(rf/reg-sub :events-row
  #(rf/subscribe [:comp])
  (fn [{e :events} [_ criteria]]
    (->> e
      (filter-keys #(topics-match? criteria %))
      (into [])
      (sort-by first >)
      (map (fn [[k {r :row}]] ^{:key k} r)))))



(rf/reg-sub :submission
  (fn [_ [_ comp-id topic value form]]
    ))

(defn- assoc-attrs [tag make-attr [a b & r :as form]]
  (if (vector? form)
    (if (= a tag)
      (if (map? b)
        (into [a (into b (make-attr form))] r)
        (into [a (make-attr form)] (rest form)))
      (mapv #(if (vector? %) (assoc-attrs tag make-attr %) %) form))
    form))

(defn- submit-form [comp-id topic form-id]
  (let [form-data (get-form-data (str "form" \# form-id))]
    (rf/dispatch [:mqtt comp-id topic form-data])))

(rf/reg-sub :submits
  #(rf/subscribe [:comp])
  (fn [{inputs :inputs c :comp-id}]
    (->> inputs
      (map
        (fn [[t i]]
          [t
           (->> i
             (assoc-attrs :button
               (fn [[_ {v :value}]] {:on-click #(rf/dispatch [:mqtt c t v])}))
             (assoc-attrs :button.form
               (fn [[_ {f-id :formid}]] {:on-click #(submit-form c t f-id)})))]))
      (sort-by first >))))