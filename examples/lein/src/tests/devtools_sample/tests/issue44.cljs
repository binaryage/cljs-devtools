(ns devtools-sample.tests.issue44
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/issue44.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->
(js/console.log {:a false})
(js/console.log {:a true})
(js/console.log [false])
(js/console.log false)

; <-- MEAT STOPS HERE ---
