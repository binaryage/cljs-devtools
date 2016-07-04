(ns devtools-sample.issue21
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/demo/devtools_sample/issue21.cljs")

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
                  (.getDate obj)]))))

(extend-dates)

;; Does not work properly
(.log js/console {:date (goog.date.Date.)})

;; Sort of works but not really
(.log js/console (goog.date.Date.))

;; Works but no cljs-devtools sugar
(println (goog.date.Date.))

; <-- MEAT STOPS HERE ---
