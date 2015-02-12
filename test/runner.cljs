(ns devtools.test-runner
  (:require [cljs.test :as test :refer-macros [run-tests] :refer [report]]
            [devtools.core-test :as core-test]))

(enable-console-print!)

(defmethod report [::test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
    (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  (aset js/window "test-failures" (+ (:fail m) (:error m))))

(test/run-tests
  (cljs.test/empty-env ::test/default)
  'devtools.core-test)
