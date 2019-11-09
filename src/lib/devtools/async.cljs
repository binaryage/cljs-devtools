(ns devtools.async
  (:require-macros [devtools.oops :refer [oset ocall]])
  (:require [goog.async.nextTick]
            [goog.labs.userAgent.browser :as ua]
            [devtools.context :as context]))

(defn ^:dynamic available? []
  (exists? js/Promise))

(def ^:dynamic fixed-chrome-version-for-async "65.0.3321")

(defn ^:dynamic needed? []
  (not (and (ua/isChrome) (ua/isVersionOrHigher fixed-chrome-version-for-async))))

(defn ^:dynamic get-not-needed-message []
  (str "cljs-devtools: the :async feature is no longer needed since Chrome " fixed-chrome-version-for-async ", "
       "see https://github.com/binaryage/cljs-devtools/issues/20"))

(def ^:dynamic *installed* false)
(def ^:dynamic *original-set-immediate* nil)

; see http://stackoverflow.com/a/30741722/84283
(defn rethrow-outside-promise [e]
  (js/setTimeout #(throw e) 0))

(defn promise-based-set-immediate [callback]
  (-> (ocall js/Promise "resolve")
      (ocall "then" callback)
      (ocall "catch" rethrow-outside-promise))
  nil)

(defn install-async-set-immediate! []
  (set! *original-set-immediate* js/goog.async.nextTick.setImmediate_)
  (set! js/goog.async.nextTick.setImmediate_ promise-based-set-immediate))

(defn uninstall-async-set-immediate! []
  (set! js/goog.async.nextTick.setImmediate_ *original-set-immediate*))

; -- installation -----------------------------------------------------------------------------------------------------------

(defn installed? []
  *installed*)

(defn install! []
  (when-not *installed*
    (set! *installed* true)
    (oset js/Error ["stackTraceLimit"] js/Infinity)
    (install-async-set-immediate!)
    (when-not (needed?)
      (.info (context/get-console) (get-not-needed-message)))
    true))

(defn uninstall! []
  (when *installed*
    (set! *installed* false)
    (uninstall-async-set-immediate!)))
