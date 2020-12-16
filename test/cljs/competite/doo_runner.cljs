(ns competite.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [competite.core-test]))

(doo-tests 'competite.core-test)

