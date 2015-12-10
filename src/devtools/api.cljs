(ns devtools.api
  (:require-macros [devtools.util :refer [oget ocall]])
  (:require [goog.object]
            [clojure.string :as string]))

; note we don't want to require figwheel here to force dependency on it for all cljs-devtools consumers
; instead we do lazy binding during runtime

(def wrapper-template
  "(try
     (js/devtools.api.repl_result {request-id} {code})
     (catch :default e
       (js/devtools.api.repl_exception {request-id} e)))")

(defn console-log [& args]
  (.apply (.-log js/console) js/console (apply array args)))

; $REPL-RESULT$ is a magic marker, cljs-devtools-shell will treat it differently
(defn log-repl-result [request-id & args]
  (apply console-log (concat ["$REPL-RESULT$" request-id] args)))

(defn ^:export repl-eval [request-id code]
  (if-let [figwheel-ns (oget js/window "figwheel")]
    (if-let [figwheel-repl-ns (oget figwheel-ns "client" "repl")]
      (if (ocall figwheel-repl-ns "is_repl_connected")
        (if (ocall figwheel-repl-ns "is_repl_available")
          (let [wrapped-code (-> wrapper-template
                                 (string/replace "{request-id}" request-id)
                                 (string/replace "{code}" code))]
            (ocall figwheel-repl-ns "repl_eval" request-id wrapped-code code))
          (log-repl-result request-id "Figwheel is connected, but REPL functionality is not available. The REPL feature is enabled by default, but your configuration might have disabled it."))
        (log-repl-result request-id "Figwheel is present in your project, but not connected to Figwheel server. Please follow Figwheel docs on how to setup it properly => https://github.com/bhauman/lein-figwheel"))
      (log-repl-result request-id "An old version of Figwheel is present. Please integrate the latest Figwheel into your project => https://github.com/bhauman/lein-figwheel"))
    (log-repl-result request-id "Figwheel is not present. Please integrate Figwheel into your project => https://github.com/bhauman/lein-figwheel")))

(defn ^:export repl-result [request-id value]
  (log-repl-result request-id value)
  value)

(defn ^:export repl-exception [request-id exception]
  (log-repl-result request-id exception)
  (throw exception))