(ns devtools.core
  (:require [goog.json :as json]
            [devtools.debug :as debug]
            [devtools.format :as format]))

(def ^:dynamic *devtools-installed* false)
(def ^:dynamic *original-formatter* nil)

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *monitor-enabled* false)

(def formatter-key "devtoolsFormatter")

(def api-mapping [["header" format/header-api-call]
                  ["hasBody" format/has-body-api-call]
                  ["body" format/body-api-call]])

(defn monitor-api-calls [name api-call]
  (fn [value]
    (if (not *monitor-enabled*)
      (api-call value)                                      ; raw API call
      (do
        (debug/log (debug/logger name) value)
        (debug/indent!)
        (let [api-response (api-call value)                 ; wrapped API call
              api-response-filter (fn [key value] (if (= key "object") (str "REF -> " (str value)) value))]
          (debug/log (debug/logger name) (str "=> " (json/serialize api-response api-response-filter)))
          (debug/unindent!)
          api-response)))))

(defn sanitize
  "wraps our api-call in try-catch block to prevent leaking of exceptions if something goes wrong"
  [_ api-call]
  (fn [value]
    (if (not *sanitizer-enabled*)
      (api-call value)                                      ; raw API call
      (try
        (api-call value)                                    ; wrapped API call
        (catch js/Object e
          (debug/log-exception e)
          nil)))))

(defn chain
  "chains our api-call with original formatter"
  [name api-call]
  (let [maybe-call-original-formatter (fn [value]
                                        (if (not (nil? *original-formatter*))
                                          (do               ; TODO should we wrap this in try-catch instead?
                                            (if *monitor-enabled* (debug/log-info "passing call to original formatter"))
                                            (.call (aget *original-formatter* name) *original-formatter* value))))]
    (fn [value]
      (if (and *devtools-enabled* (format/want-value? value))
        (api-call value)
        (maybe-call-original-formatter value)))))

(defn cljs-formatter []
  (let [api-call-wrapper (fn [name api-call]
                           (let [monitor (partial monitor-api-calls name)
                                 chainer (partial chain name)
                                 sanitizer (partial sanitize name)
                                 composition (comp monitor chainer sanitizer)]
                             (composition api-call)))
        api-gen (fn [[name api-call]] [name (api-call-wrapper name api-call)])]
    (apply js-obj (mapcat #(api-gen %) api-mapping))))

(defn install! []
  (if *devtools-installed*
    (debug/log-info "devtools already installed - nothing to do")
    (do
      (set! *devtools-installed* true)
      (set! *original-formatter* (aget js/window formatter-key))
      (aset js/window formatter-key (cljs-formatter)))))

(defn uninstall! []
  "this may be not safe if someone chained their formatter after us"
  (aset js/window formatter-key *original-formatter*)
  (set! *original-formatter* nil)
  (set! *devtools-installed* false))

(defn disable! []
  (set! *devtools-enabled* false))

(defn enable! []
  (set! *devtools-enabled* true))