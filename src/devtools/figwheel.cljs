(ns devtools.figwheel
  (:require-macros [devtools.util :refer [oget ocall oapply]])
  (:require [goog.object]))

; this should provide interace between figwheel's REPL driver and cljs-devtools

; note we don't want to require figwheel here to force dependency on it for all cljs-devtools consumers
; instead we do lazy binding at runtime

(def ^:dynamic repl-not-available-msg
  (str "Figwheel is connected, but REPL functionality is not available. "
       "The REPL feature is enabled by default, but your configuration might have disabled it."))

(def ^:dynamic repl-not-connected-msg
  (str "Figwheel is present in your project, but not connected to Figwheel server. "
       "Please follow Figwheel docs on how to setup it properly => https://github.com/bhauman/lein-figwheel"))

(def ^:dynamic old-figwheel-msg
  (str "An old version of Figwheel is present. "
       "Please integrate the latest Figwheel into your project => https://github.com/bhauman/lein-figwheel"))

(def ^:dynamic missing-figwheel-msg
  (str "Figwheel is not present in this javascript context. "
       "Please integrate Figwheel into your project => https://github.com/bhauman/lein-figwheel"))

(defn log-warning [& args]
  (.apply (.-warn js/console) js/console (apply array args)))

(defn get-repl-driver-ns []
  (oget js/window "figwheel" "plugin" "repl_driver"))

(defn figwheel-driver-problem? []
  (if (oget js/window "figwheel")
    (if-let [repl-driver-ns (get-repl-driver-ns)]
      (if (ocall repl-driver-ns "is_socket_connected")
        (if (ocall repl-driver-ns "is_repl_available")
          nil
          repl-not-available-msg)
        repl-not-connected-msg)
      old-figwheel-msg)
    missing-figwheel-msg))

(defn call-figwheel-driver [name & args]
  (if-let [reason (figwheel-driver-problem?)]
    (log-warning reason)
    (oapply (get-repl-driver-ns) name args)))

(defn subscribe! [callback]
  (if-let [repl-driver-ns (get-repl-driver-ns)]
    (ocall repl-driver-ns "subscribe" callback)))

(defn unsubscribe! [callback]
  (if-let [repl-driver-ns (get-repl-driver-ns)]
    (ocall repl-driver-ns "unsubscribe" callback)))