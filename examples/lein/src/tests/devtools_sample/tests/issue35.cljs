(ns devtools-sample.tests.issue35
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/issue35.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(def x [])
(alter-meta! x #(assoc % :ref x))
(log x)

; <-- MEAT STOPS HERE ---
