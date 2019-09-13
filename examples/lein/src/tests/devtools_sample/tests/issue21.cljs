(ns devtools-sample.tests.issue21
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/issue21.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(defn extend-dates
  "Adds print instructions to goog.date types"
  []
  (extend-protocol IPrintWithWriter
    goog.date.Date
    (-pr-writer [obj writer _opts]
      (write-all writer
                 "#gdate "
                 [(.getYear obj)
                  (.getMonth obj)
                  (.getDate obj)]
                 #js ["test"]
                 (js-obj 'test "js-obj")
                 :keyword
                 'sym
                 42
                 #"regex"))))

(extend-dates)

(.log js/console "a Date object embedded in a map:" {:date (goog.date.Date.)})
(.log js/console "a raw Date object:" (goog.date.Date.))
(.log js/console "pr-str of a raw Date object:" (pr-str (goog.date.Date.)))
(println "println of a raw Date object:" (goog.date.Date.))

; <-- MEAT STOPS HERE ---
