(ns devtools.api
  (:require-macros [devtools.util :refer [oget ocall oapply]])
  (:require [goog.object]
            [clojure.string :as string]
            [devtools.figwheel :refer [figwheel-driver-problem? call-figwheel-driver]]
            [devtools.dirac :as dirac]))

; keep in mind that we want to avoid any state at all
; javascript running this code can be reloaded anytime, same with devtools front-end

(def ^:dynamic wrapper-template
  "(try
     (js/devtools.api.repl_result {request-id} {code})
     (catch :default e
       (js/devtools.api.repl_exception {request-id} e)))")

(defn ^:export eval [request-id code]
  (let [wrapped-code (-> wrapper-template
                         (string/replace "{request-id}" request-id)
                         (string/replace "{code}" code))]
    (call-figwheel-driver "eval" request-id wrapped-code code)))

(defn ^:export repl-result [request-id value]
  (dirac/log request-id "result" value)
  value)

(defn ^:export repl-exception [request-id exception]
  (dirac/error request-id "exception" exception)
  (throw exception))

(defn ^:export warm-up-repl-connection []
  (if-let [reason (figwheel-driver-problem?)]
    (.warn js/console reason)
    (do
      (call-figwheel-driver "request_ns")
      true)))