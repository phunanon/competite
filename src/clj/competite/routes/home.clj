(ns competite.routes.home
  (:require
    [competite.layout :as layout]
    
    [competite.db.account  :as account]
    [competite.db.lists    :as lists]
    [competite.db.polling  :as poll]

    [clojure.java.io :as io]
    [competite.middleware :as middleware]
    [ring.util.http-response :as resp]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.gzip :refer [wrap-gzip]]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn handle-init
  [{{:keys [user-id]} :session}]
  (let [res (resp/ok {:home-md (-> "docs/home.md" io/resource slurp)
                      :comps (lists/all-comps)})]
    (if user-id
      (into res {:user (select-keys (account/search-user :id user-id) [:id :email :name])})
      res)))

(defn handle-signin
  [{{:strs [email pass]} :params
    session              :session :as req}]
  (let [{:keys [id] :as user-result}
                  (account/email+pass->user email pass)
        is-authed (not (#{:bad-email :bad-pass} user-result))
        response  {(if id :user :signin) user-result}
        response  (into response (if is-authed {:comps (lists/all-comps id)} {}))
        response  (resp/ok response)]
    (if is-authed
      (into response {:session (into session {:user-id id})})
      response)))

(defn handle-signout
  [{:keys [session]}]
  (-> (resp/ok {:session (dissoc session :user-id)})))

(defn handle-register
  [{{:keys [email pass]} :params}]
  (let [new-id (account/new-user! email pass)]
    (if new-id
      (handle-signin
        {:params {:email email :pass pass}}))))

(defn handle-poll
  [{{c :comp-id n-e :num-event do-i :give-inputs} :params}]
  (let [response {:comp-id c}
        events   {:events (poll/records-from c :events n-e)}
        inputs   (if do-i {:inputs (poll/records-from c :inputs 0)})
        response (reduce into response [events inputs])]
    (resp/ok response)))

(defn handle-mqtt
  [{{[topic payload] :mqtt c :comp-id} :params}]
  (poll/merge-event c topic payload)
  (resp/ok))

(defn handle-schema [{schema :params}]
  (poll/merge-schema schema)
  (resp/ok))

(defn serve-token [_]
  (-> (resp/ok (poll/model-token))
      (resp/header "Content-Type" "text/plain; charset=utf-8")))

(defn serve-docs [_]
  (-> (resp/ok (-> "docs/docs.md" io/resource slurp))
      (resp/header "Content-Type" "text/plain; charset=utf-8")))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats
                 #(wrap-restful-format % :formats [:edn])
                 wrap-gzip]}
   ["/"         {:get  home-page}]
   ["/init"     {:post handle-init}]
   ["/signin"   {:post handle-signin}]
   ["/signout"  {:post handle-signout}]
   ["/register" {:post handle-register}]
   ["/poll"     {:post handle-poll}]
   ["/mqtt"     {:post handle-mqtt}]
   ["/schema"   {:post handle-schema}]
   ["/token"    {:get  serve-token}]
   ["/docs"     {:get  serve-docs}]])

