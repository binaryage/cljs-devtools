(ns devtools-sample.tests.hints
  (:require-macros [devtools-sample.logging :refer [log info]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! <! chan timeout alts! close!]]
            [devtools-sample.boot :refer [boot!]]
            [devtools.core]
            [dirac.runtime]))

(boot! "/src/tests/devtools_sample/tests/hints.cljs")

; --- MEAT STARTS HERE -->

(defn get-nil [])

(defn ^:export trigger! []
  ((get-nil) 1 2 3))

(trigger!)

; <-- MEAT STOPS HERE ---
