(ns competite.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [competite.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[competite started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[competite has shut down successfully]=-"))
   :middleware wrap-dev})
