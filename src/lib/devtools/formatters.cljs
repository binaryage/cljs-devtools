(ns devtools.formatters
  (:require [goog.labs.userAgent.browser :as ua]
            [devtools.prefs :as prefs]
            [devtools.util :refer [get-formatters-safe set-formatters-safe! in-node-context?]]
            [devtools.context :as context]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]))

(def ^:dynamic *installed* false)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *monitor-enabled* false)

(def obsolete-formatter-key "devtoolsFormatter")

(defn ^:dynamic available? []
  (or (in-node-context?)                                                                                                      ; node.js or Chrome 47+
      (and (ua/isChrome) (ua/isVersionOrHigher 47))))

(deftype CLJSDevtoolsFormatter [])

; devtools.debug namespace may not be present => no debugging
(defn- find-fn-in-debug-ns [fn-name]
  (try
    (aget (context/get-root) "devtools" "debug" fn-name)
    (catch :default _
      nil)))

(defn- monitor-api-call-if-avail [name api-call args]
  (if-let [monitor-api-call (find-fn-in-debug-ns "monitor_api_call")]
    (monitor-api-call name api-call args)
    (apply api-call args)))

(defn- log-exception-if-avail [& args]
  (if-let [log-exception (find-fn-in-debug-ns "log_exception")]
    (apply log-exception args)))

; monitors api calls in a separate debug console if debug namespace is available
(defn- monitor-api-calls [name api-call]
  (fn [& args]
    (if-not *monitor-enabled*
      (apply api-call args)
      (monitor-api-call-if-avail name api-call args))))

; wraps our api calls in a try-catch block to prevent leaking of exceptions in case something went wrong
(defn- sanitize [name api-call]
  (fn [& args]
    (if-not *sanitizer-enabled*
      (apply api-call args)                                                                                                   ; raw API call
      (try
        (apply api-call args)                                                                                                 ; wrapped API call
        (catch :default e
          (log-exception-if-avail (str name ": " e))
          nil)))))

(defn- build-cljs-formatter []
  (let [wrap (fn [name api-call]
               (let [monitor (partial monitor-api-calls name)
                     sanitizer (partial sanitize name)]
                 ((comp monitor sanitizer) api-call)
                 api-call))
        formatter (CLJSDevtoolsFormatter.)
        define! (fn [name fn]
                  (aset formatter name (wrap name fn)))]
    (define! "header" header-api-call)
    (define! "hasBody" has-body-api-call)
    (define! "body" body-api-call)
    formatter))

(defn- is-ours? [o]
  (instance? CLJSDevtoolsFormatter o))

(defn- present? []
  (let [formatters (get-formatters-safe)]
    (boolean (some is-ours? formatters))))

(defn- install-our-formatter! [formatter]
  (let [formatters (.slice (get-formatters-safe))]                                                                            ; slice effectively duplicates the array
    (.push formatters formatter)                                                                                              ; acting on duplicated array
    (set-formatters-safe! formatters)
    (if (prefs/pref :legacy-formatter)
      (aset (context/get-root) obsolete-formatter-key formatter))))

(defn- uninstall-our-formatters! []
  (let [new-formatters (remove is-ours? (vec (get-formatters-safe)))
        new-formatters-js (if (empty? new-formatters) nil (into-array new-formatters))]
    (set-formatters-safe! new-formatters-js)))

; -- installation -----------------------------------------------------------------------------------------------------------

(defn installed? []
  *installed*)

(defn install! []
  (when-not *installed*
    (set! *installed* true)
    (install-our-formatter! (build-cljs-formatter))
    true))

(defn uninstall! []
  (when *installed*
    (set! *installed* false)
    (uninstall-our-formatters!)))
