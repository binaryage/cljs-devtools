(ns devtools.core
  (:require [devtools.debug :as debug]
            [devtools.format :as format]))

(def ^:dynamic *devtools-installed* false)
(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *sanitizer-enabled* true)
(def ^:dynamic *original-formatter* nil)

(def formatter-key "devtoolsFormatter")

(def api-mapping [["header" format/header-api-call]
                  ["hasBody" format/has-body-api-call]
                  ["body" format/body-api-call]])

(defn sanitize
  "wraps our api-call in try-catch block to prevent leaking of exceptions if something goes wrong"
  [api-call]
  (fn [value]
    (try
      (api-call value)
      (catch js/Object e
        (debug/log-exception e)
        nil))))

(defn chain
  "chains our api-call with original formatter"
  [name original-formatter enabled? api-call]
  (let [maybe-call-original-formatter (fn [value]
                                        (if (not (nil? original-formatter))
                                          (do               ; TODO should we wrap this in try-catch instead?
                                            (debug/log-info "passing call to original formatter")
                                            (.call (aget original-formatter name) original-formatter value))))]
    (fn [value]
      (if (and (enabled?) (format/want-value? value))
        (api-call value)
        (maybe-call-original-formatter value)))))

(defn cljs-formatter [formatter-enabled? original-formatter sanitizer-enabled]
  (let [api-call-wrapper (fn [name api-call]
                           (let [monitor (partial debug/api-call-monitor name)
                                 chainer (partial chain name original-formatter formatter-enabled?)
                                 sanitizer (if sanitizer-enabled sanitize identity)]
                             ((comp monitor chainer sanitizer) api-call)))
        api-gen (fn [[name api-call]] [name (api-call-wrapper name api-call)])]
    (apply js-obj (mapcat #(api-gen %) api-mapping))))

(defn install-devtools! []
  (if *devtools-installed*
    (debug/log-info "devtools already installed - nothing to do")
    (do
      (set! *devtools-installed* true)
      (set! *original-formatter* (aget js/window formatter-key))
      (aset js/window formatter-key (cljs-formatter (fn [] *devtools-enabled*) *original-formatter* *sanitizer-enabled*)))))

; NOT SAFE
(defn uninstall-devtools! []
  (aset js/window formatter-key *original-formatter*)
  (set! *original-formatter* nil)
  (set! *devtools-installed* false))

(defn disable-devtools! []
  (set! *devtools-enabled* false))

(defn enable-devtools! []
  (set! *devtools-enabled* true))