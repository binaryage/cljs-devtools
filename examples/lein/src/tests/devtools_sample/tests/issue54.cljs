(ns devtools-sample.tests.issue54
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/issue54.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->
(js/console.log ##NaN ##Inf ##-Inf)
(js/console.log [##NaN ##Inf ##-Inf])

; <-- MEAT STOPS HERE ---
