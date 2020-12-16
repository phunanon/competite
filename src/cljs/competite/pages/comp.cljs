(ns competite.pages.comp
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [medley.core :refer [filter-keys filter-kv]]
    [clojure.string :as str]
    [competite.common :refer [md->div info-tags content-div]]))

(defn <subscribe-btn>
  [header criteria]
  (print header criteria)
  (let [*is-subbed (rf/subscribe [:subbed? criteria])]
    (fn []
      [:button
        {:class "button"
        :style {:float :right}
        :onClick #(rf/dispatch (if @*is-subbed [:unsub criteria] [:sub criteria header]))}
      (if @*is-subbed "Unsubscribe" "Subscribe")])))

(defn comp-page [{:keys [id]}]
  (rf/dispatch [:select-comp id])
  (let [{:keys [title desc views] :as comp}
          @(rf/subscribe [:comp])]
    [content-div
      [:h2 title]
      (md->div desc)
      [:br]
      (for [[criteria {:keys [header headers]}] views]
        ^{:key criteria}
        [:section
          {:style {:margin-bottom "3rem"}}
          [:h3
            [<subscribe-btn> header criteria]
            header]
          [:table
            [:tbody headers
              @(rf/subscribe [:events-row criteria])]]])]))