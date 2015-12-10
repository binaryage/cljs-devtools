(ns devtools.api
  (:require-macros [devtools.util :refer [oget ocall]])
  (:require [goog.object]
            [clojure.string :as string]))

(def wrapper-template
  "(try
     (js/devtools.api.repl_result {request-id} {code})
     (catch :default e
       (js/devtools.api.repl_exception {request-id} e)))")

; TODO: error checks
(defn ^:export repl-eval [request-id code]
  (if-let [figwheel-repl-ns (oget js/window "figwheel" "client" "repl")]
    (let [wrapped-code (-> wrapper-template
                           (string/replace "{request-id}" request-id)
                           (string/replace "{code}" code))]
      (ocall figwheel-repl-ns "repl_eval" request-id wrapped-code code))))

(defn ^:export repl-result [request-id value]
  (.log js/console "$REPL-RESULT$" request-id value)
  value)

(defn ^:export repl-exception [request-id exception]
  (.log js/console "$REPL-RESULT$" request-id exception)
  (throw exception))

