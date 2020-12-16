(ns competite.core
  (:require
    [competite.common :refer [md->div content-div get-form-data e-val]]
    [competite.pages.comp :as comp]
    [competite.pages.exercise :as exer]
    [competite.ajax :as ajax] ;;;;
    [competite.events :as events] ;;;;
    [day8.re-frame.http-fx] ;;;;
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as goog-events]
    [goog.history.EventType :as HistoryEventType]
    [reitit.core :as reitit]
    [clojure.string :as str])
  (:import goog.History))

(defn nav-link [is-open uri title page]
  [:a.navbar-item
   {:href    uri
    :class   (when (= page @(rf/subscribe [:page])) :is-active)
    :onClick #(reset! is-open false)}
   title])

(defn <navbar> []
  (r/with-let [is-open  (r/atom false)
               nav-link (partial nav-link is-open)]
    [:nav.navbar
      {:class (if @(rf/subscribe [:db :http-bad]) "is-danger" "is-info")
       :style {:transition "all .05s ease-in-out"}}
      [:div.container
        [:div.navbar-brand
          [nav-link "#/" "Competite" :home]
          [:progress.progress.is-small.is-primary
            {:style
              {:margin "auto"
               :width "5rem"
               :opacity (if @(rf/subscribe [:db :http-wait]) 1 0)}}]
          [:span.navbar-burger.burger
            {:data-target :nav-menu
             :on-click #(swap! is-open not)
             :class (when @is-open :is-active)}
            [:span][:span][:span]]]
        [:div#nav-menu.navbar-menu
          {:class (when @is-open :is-active)}
          (let [{:keys [comp-id title] :as comp}
                  @(rf/subscribe [:comp])
                comps    @(rf/subscribe [:db-map :comps])
                drop-div :div.navbar-item.has-dropdown.is-hoverable
                user     @(rf/subscribe [:db :user])]
            [:div.navbar-start
              [drop-div
                [:a.navbar-link (if comp title "Competitions")]
                [:div.navbar-dropdown
                  (for [[id {:keys [title] :as comp-info}] comps]
                    ^{:key id}
                    [:a.navbar-item
                      {:href (str "#/comp/" id)
                        :onClick #(reset! is-open false)
                        :class (if (= id comp-id) :is-active)}
                      title])]]
              (if comp
                [drop-div
                  [:a.navbar-link "Notifications"]
                  (let [notis @(rf/subscribe [:notis])]
                    [:div.navbar-dropdown
                      ;[nav-link "#/notis" "All notifications" :notis]
                      [nav-link "#/subs" "Mange subscriptions" :subs]
                      (if (seq notis) [:hr {:class "navbar-divider"}])
                      (for [[id text] notis]
                        ^{:key id}
                        [:a.navbar-item
                          {;:href (str "#/noti/" id)
                           :onClick #(reset! is-open false)}
                          text])])])
              (if (and user comp) [nav-link "#/submit" "Submit" :submit])
              (let [{:keys [name]} @(rf/subscribe [:db :user])
                    name (if name name "Sign in")]
                [nav-link "#/sign" name :sign])])]]]))


(defn <notification> []
  (r/with-let [last-noti (r/atom [false 0])]
    (let [[[noti-ms noti-text]] @(rf/subscribe [:notis 1])
          [is-open remote-ms] @last-noti]
      (when (not= remote-ms noti-ms)
        (js/setTimeout #(reset! last-noti [false noti-ms]) 5000)
        (reset! last-noti [true noti-ms]))
      [:div
        {:class "notification is-link"
        :style {:display (if is-open "inline-block" "none")
                :position "fixed"
                :left "1rem"
                :bottom "1rem"}}
        noti-text])))

(defn home-page []
  [content-div
   (when-let [home-md @(rf/subscribe [:db :home-md])]
     (md->div home-md))])


(defn sign-page []
  (let [status       @(rf/subscribe [:db :signin])
        {:keys [name] :as user}
                     @(rf/subscribe [:db :user])
        form-data    #(get-form-data "form#signin")
        sign-or-reg! #(rf/dispatch [:sign-or-reg % (form-data)])
        signin!      #(sign-or-reg! "/signin")
        register!    #(sign-or-reg! "/register")
        signout!     #(rf/dispatch [:signout])]
    [content-div
      (if (or (#{"bad-email" "bad-pass"} user)
              (nil? user))
        [:div
          (if status [:p.notification.is-warning status])
          [:form#signin
            {:method "post" :onSubmit (fn [e] (.preventDefault e))}
            [:input.input {:name "email" :placeholder "email"
                           :required true}] [:br]
            [:input.input {:name "pass" :placeholder "password" :type "password"
                          :required true}]
            [:br] [:br]
            [:button.button {:type "button" :onClick signin!} "sign in"]
            [:button.button {:type "button" :onClick register!} "register"]]]
        [:div
          [:p [:b "Hello, " name]]
          [:button.button {:onClick signout!} "sign out"]])]))


(defn notis-page []
  [content-div "nothing"])

(defn subs-page []
  [content-div
    [:h3 "Your subscriptions"]
    [:section
      [:p
        [:b "Add a subscription: "]
        [:input {:id "newSub" :placeholder "+/sport/event/name/#"}]
        [:button {:on-click #(rf/dispatch [:sub (e-val "#newSub") "Custom"])} "Add"]]]
    [:table
      [:tbody
        [:tr [:th "Name"] [:th "Pattern"]]
        (for [[topic name] @(rf/subscribe [:db-map :subs])]
          ^{:key topic}
          [:tr
            [:td
              [:button {:on-click #(rf/dispatch [:unsub topic])} "Unsubscribe"]
              " " name] [:td topic]])]]])

(defn submit-page []
  [content-div
    [:h3 "Submissions"]
    [:p "As you are signed in you have access to any inputs available to your role, per competition."]
    [:table
      [:tbody
        [:tr [:th "Form"]]
        (for [[pattern form] @(rf/subscribe [:submits])]
          ^{:key pattern}
          [:tr [:td form]])]]])


;; Pages

(def pages
  {:home     #'home-page
   :comp     comp/comp-page
   ;:notis    #'notis-page
   :subs     #'subs-page
   :submit   #'submit-page
   :sign     #'sign-page})

(defn page []
  [:div
    [<navbar>]
    (let [route   @(rf/subscribe [:route])
          handler (-> route :data :name pages)
          params  (-> route :path-params)]
      [handler params])
    [<notification>]])

;; Routes

(def router
  (reitit/router
    [["/"         :home]
     ["/comp/:id" {:name :comp :parameters {:path {:id int?}}}]
     ;["/notis"    :notis]
     ["/subs"     :subs]
     ["/submit"   :submit]
     ["/sign"     :sign]]))

;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (goog-events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (let [uri (or (not-empty (str/replace (.-token event) #"^.*#" "")) "/")]
          (rf/dispatch
            [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
  (ajax/load-interceptors!)
  (rf/dispatch [:http "/init"])

  ;Configure polling
  (js/setInterval #(rf/dispatch [:poll-server]) 3000)

  (hook-browser-navigation!)
  (mount-components))
