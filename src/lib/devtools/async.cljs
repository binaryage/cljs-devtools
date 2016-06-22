(ns devtools.async
  (:require-macros [devtools.util :refer [oget oset ocall oapply]])
  (:require [goog.async.nextTick :as next-tick]))

(defn ^:dynamic available? []
  (exists? js/Promise))

(def ^:dynamic *installed* false)
(def ^:dynamic *original-set-immediate* nil)

(defn promise-based-set-immediate [callback]
  (-> (ocall js/Promise "resolve")
      (ocall "then" callback))
  nil)

(defn install-async-set-immediate! []
  (set! *original-set-immediate* next-tick/setImmediate_)
  (set! next-tick/setImmediate_ promise-based-set-immediate))

(defn uninstall-async-set-immediate! []
  (set! next-tick/setImmediate_ *original-set-immediate*))

; -- installation -----------------------------------------------------------------------------------------------------------

(defn installed? []
  *installed*)

(defn install! []
  (when-not *installed*
    (set! *installed* true)
    (oset js/Error ["stackTraceLimit"] js/Infinity)
    (install-async-set-immediate!)
    true))

(defn uninstall! []
  (when *installed*
    (set! *installed* false)
    (uninstall-async-set-immediate!)))
