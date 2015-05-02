(ns devtools.core
  (:require [goog.json :as json]
            [devtools.debug :as debug]
            [devtools.format :as format]))

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *monitor-enabled* false)

(def formatter-key "devtoolsFormatters")

(deftype CLJSDevtoolsFormatter [header hasBody body])

(defn- monitor-api-calls [name api-call]
  (fn [value]
    (if-not *monitor-enabled*
      (api-call value)                                      ; raw API call
      (do
        (debug/log (debug/logger name) value)
        (debug/indent!)
        (let [api-response (api-call value)                 ; wrapped API call
              api-response-filter (fn [key value] (if (= key "object") "##REF##" value))]
          (debug/log (debug/logger name) (str "=> " (js->clj (json/parse (json/serialize api-response api-response-filter)))))
          (debug/unindent!)
          api-response)))))

(defn- sanitize
  "wraps our api-call in try-catch block to prevent leaking of exceptions if something goes wrong"
  [_ api-call]
  (fn [value]
    (if-not *sanitizer-enabled*
      (api-call value)                                      ; raw API call
      (try
        (api-call value)                                    ; wrapped API call
        (catch :default e
          (debug/log-exception e)
          nil)))))

(defn- build-cljs-formatter []
  (let [api-call-wrapper (fn [name api-call]
                           (let [monitor (partial monitor-api-calls name)
                                 sanitizer (partial sanitize name)]
                             ((comp monitor sanitizer) api-call)))]
    (CLJSDevtoolsFormatter.
      (api-call-wrapper "header" format/header-api-call)
      (api-call-wrapper "hasBody" format/has-body-api-call)
      (api-call-wrapper "body" format/body-api-call))))

(defn- is-ours? [o]
  (instance? CLJSDevtoolsFormatter o))

(defn- get-formatters-safe []
  (let [formatters (aget js/window formatter-key)]
    (if (array? formatters)                                 ; TODO: maybe issue a warning if formatters are anything else than array or nil
      formatters
      #js [])))

(defn- installed? []
  (let [formatters (get-formatters-safe)]
    (boolean (some is-ours? formatters))))

(defn- install-our-formatter! [formatter]
  (let [formatters (.slice (get-formatters-safe))]          ; slice effectively duplicates the array
    (.push formatters formatter)                            ; acting on duplicated array
    (aset js/window formatter-key formatters)))

(defn- uninstall-our-formatters! []
  (let [new-formatters (remove is-ours? (vec (get-formatters-safe)))
        new-formatters-js (if (empty? new-formatters) nil (into-array new-formatters))]
    (aset js/window formatter-key new-formatters-js)))

(defn install! []
  (if (installed?)
    (debug/log-info "devtools already installed - nothing to do")
    (install-our-formatter! (build-cljs-formatter))))

(defn uninstall! []
  (if-not (installed?)
    (debug/log-info "devtools not installed - nothing to do")
    (uninstall-our-formatters!)))


(defn disable! []
  (set! *devtools-enabled* false))

(defn enable! []
  (set! *devtools-enabled* true))