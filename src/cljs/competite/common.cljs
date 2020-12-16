(ns competite.common
  (:require
    [markdown.core :refer [md->html]]))

(defn e-val [selector]
  (.-value (.querySelector js/document selector)))

(def content-div :section.section>div.container>div.content)

(defn md->div [content]
  [:div {:dangerouslySetInnerHTML {:__html (md->html content)}}])

(defn info-tags [tags]
  [:div.tags
    (for [text tags]
      ^{:key text}
      [:span.tag.is-info text])])

(defn get-form-data [el-name]
  (->> el-name
    (.querySelector js/document)
    (js/FormData.)
    (.fromEntries js/Object)
    (js->clj)))