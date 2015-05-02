(ns devtools.core
  (:require [goog.json :as json]
            [devtools.debug :as debug]
            [devtools.format :as format]))

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *monitor-enabled* false)

(def formatter-key "devtoolsFormatters")
(def api-marker "cljs_devtools_handler")

(def api-mapping [["header" format/header-api-call]
                  ["hasBody" format/has-body-api-call]
                  ["body" format/body-api-call]])

(defn monitor-api-calls [name api-call]
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

(defn sanitize
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

(defn cljs-formatter []
  (let [api-call-wrapper (fn [name api-call]
                           (let [monitor (partial monitor-api-calls name)
                                 sanitizer (partial sanitize name)]
                             ((comp monitor sanitizer) api-call)))
        api-gen (fn [[name api-call]] [name (api-call-wrapper name api-call)])]
    (apply js-obj (mapcat #(api-gen %) api-mapping))))

(defn- is-marked? [o]
  (boolean (aget o api-marker)))

(defn- mark! [o]
  (aset o api-marker true)
  o)

(defn- safe-get-formatters []
  (let [formatters (aget js/window formatter-key)]
    (if (array? formatters)                                 ; TODO: maybe issue a warning if formatters are anything else than array or nil
      formatters
      #js [])))

(defn- installed? []
  (let [formatters (safe-get-formatters)]
    (boolean (some is-marked? formatters))))

(defn- install-marked-formatter! [formatter]
  (let [formatters (.slice (safe-get-formatters))]          ; slice effectively duplicates the array
    (.push formatters formatter)                            ; acting on duplicated array
    (aset js/window formatter-key formatters)))

(defn- uninstall-marked-formatters! []
  (let [formatters (safe-get-formatters)
        new-formatters (remove is-marked? (vec formatters))
        new-formatters-value (if (empty? new-formatters) nil (into-array new-formatters))]
    (aset js/window formatter-key new-formatters-value)))

(defn install! []
  (if (installed?)
    (debug/log-info "devtools already installed - nothing to do")
    (install-marked-formatter! (mark! (cljs-formatter)))))

(defn uninstall! []
  (if-not (installed?)
    (debug/log-info "devtools not installed - nothing to do")
    (uninstall-marked-formatters!)))


(defn disable! []
  (set! *devtools-enabled* false))

(defn enable! []
  (set! *devtools-enabled* true))