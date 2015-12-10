(ns devtools.core
  (:require [devtools.prefs :as prefs]
            [devtools.format :as format]
            [devtools.sanity-hints :as hints]
            [devtools.api]))

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *monitor-enabled* false)

(def formatter-key "devtoolsFormatters")
(def obsolete-formatter-key "devtoolsFormatter")

(deftype CLJSDevtoolsFormatter [])

; devtools.debug namespace may not be present => no debugging
(defn- find-fn-in-debug-ns [fn-name]
  (try
    (aget js/window "devtools" "debug" fn-name)
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
      (apply api-call args)                                                                                           ; raw API call
      (try
        (apply api-call args)                                                                                         ; wrapped API call
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
    (define! "header" format/header-api-call)
    (define! "hasBody" format/has-body-api-call)
    (define! "body" format/body-api-call)
    formatter))

(defn- is-ours? [o]
  (instance? CLJSDevtoolsFormatter o))

(defn- get-formatters-safe []
  (let [formatters (aget js/window formatter-key)]
    (if (array? formatters)                                                                                           ; TODO: maybe issue a warning if formatters are anything else than array or nil
      formatters
      #js [])))

(defn- installed? []
  (let [formatters (get-formatters-safe)]
    (boolean (some is-ours? formatters))))

(defn- install-our-formatter! [formatter]
  (let [formatters (.slice (get-formatters-safe))]                                                                    ; slice effectively duplicates the array
    (.push formatters formatter)                                                                                      ; acting on duplicated array
    (aset js/window formatter-key formatters)
    (if (prefs/pref :legacy-formatter)
      (aset js/window obsolete-formatter-key formatter))))

(defn- uninstall-our-formatters! []
  (let [new-formatters (remove is-ours? (vec (get-formatters-safe)))
        new-formatters-js (if (empty? new-formatters) nil (into-array new-formatters))]
    (aset js/window formatter-key new-formatters-js)))

; -- public API -----------------------------------------------------------------------------------------------------

(defn install! []
  (if (installed?)
    (.warn js/console "install!: devtools already installed - nothing to do")
    (do
      (install-our-formatter! (build-cljs-formatter))
      (when (prefs/pref :install-sanity-hints)
        (hints/install!)))))

(defn uninstall! []
  (if-not (installed?)
    (.warn js/console "uninstall!: devtools not installed - nothing to do")
    (do
      (uninstall-our-formatters!)
      (if (prefs/pref :install-sanity-hints)
        (hints/uninstall!)))))

(defn disable! []
  (set! *devtools-enabled* false))

(defn enable! []
  (set! *devtools-enabled* true))

(defn set-prefs! [new-prefs]
  (prefs/set-prefs! new-prefs))

(defn get-prefs []
  (prefs/get-prefs))

(defn set-pref! [pref val]
  (prefs/set-pref! pref val))