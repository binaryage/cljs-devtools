(ns devtools-sample.async
  (:require-macros [devtools-sample.logging :refer [log info]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! <! chan timeout alts! close!]]
            [devtools-sample.boot :refer [boot!]]
            [devtools.core]
            [dirac.runtime]))

(boot! "/src/demo/devtools_sample/async.cljs")

;(devtools.core/uninstall!)
;(dirac.runtime/install!)

; --- MEAT STARTS HERE -->

(defn break-here! []
  (js-debugger))

(defn break-async []
  (go
    (<! (timeout 1000))
    (break-here!)))

(defn break-loop-async [n]
  (go-loop [i 0]
    (if (> i n)
      (break-here!)
      (do
        (<! (timeout 100))
        (recur (inc i))))))


; <-- MEAT STOPS HERE ---

(defn ^:export break-async-handler []
  (break-async))

(defn ^:export break-loop-async-handler []
  (break-loop-async 20))
