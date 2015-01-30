(ns devtools.core
  (:require [devtools.debug :as debug]))

(def max-seq-elements 10)
(def max-map-elements 5)
(def max-set-elements 7)
(def abbreviation-string "…")
(def line-index-separator ":")
(def formatter-key "devtoolsFormatter")

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *original-formatter* nil)

(declare inlined-value-template)

; dirty
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "_hash"))))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn template [tag style & more]
  (let [arr #js [tag (if (empty? style) #js {} #js {"style" style})]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn reference [object & more]
  (let [arr #js ["object" #js {"object" object}]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn spacer [& _] " ")

(defn nil-template [_]
  (template "span" "color:#808080" "nil"))

(defn keyword-template [value]
  (template "span" "color:#881391" (str ":" (name value))))

(defn symbol-template [value]
  (template "span" "color:#881391" (str value)))

(defn number-template [value]
  (if (integer? value)
    (template "span" "color:#1C00CF" value)
    (template "span" "color:#1C88CF" value)))

(defn index-template [value]
  (template "span" "color:#881391" value line-index-separator))

(defn deref-template [value]
  (cond
    (satisfies? IAtom value) (template "span" "color:#f0f" "#<Atom " (reference @value) ">")
    (satisfies? IVolatile value) (template "span" "color:#f0f" "#<Volatile " (reference @value) ">")
    :else (pr-str value))) ; TODO: should we handle IDelay and others? I believe it is not safe to dereference them here

; TODO: abbreviate long strings
(defn string-template [value]
  (template "span" "color:#C41A16" (str "\"" value "\"")))

(defn fn-template [value]
  (template "span" "color:#f00" (reference value) "fn"))

; TODO: convert to idiomatic clojure code
(defn header-seq-template [value]
  (let [arr (template "span" "color:#000" "[")]
    (doseq [x (take max-seq-elements value)]
      (.push arr (inlined-value-template x) (spacer x)))
    (.pop arr)
    (if (> (count value) max-seq-elements)
      (.push arr abbreviation-string))
    (.push arr "]")
    arr))

(defn header-map-template [value]
  (let [arr (template "span" "color:#000" "{")
        v (seq value)]
    (doseq [[k v] (take max-map-elements v)]
      (.push arr
             (inlined-value-template k) (spacer k)
             (inlined-value-template v) (spacer v)))
    (.pop arr)
    (if (> (count v) max-map-elements)
      (.push arr abbreviation-string))
    (.push arr "}")
    arr))

(defn header-set-template [value]
  (let [arr (template "span" "color:#000" "#{")]
    (doseq [x (take max-set-elements value)]
      (.push arr (inlined-value-template x) (spacer x)))
    (.pop arr)
    (if (> (count value) max-set-elements)
      (.push arr abbreviation-string))
    (.push arr "}")
    arr))

(defn generic-template [value]
  (template "span" "color:#000" (reference value)))

(defn atomic-template [value]
  (cond
    (nil? value) (nil-template value)
    (string? value) (string-template value)
    (number? value) (number-template value)
    (keyword? value) (keyword-template value)
    (symbol? value) (symbol-template value)
    (fn? value) (fn-template value)
    (satisfies? IDeref value) (deref-template value)
    ))

(defn header-container-template [value]
  (cond
    (map? value) (header-map-template value)
    (set? value) (header-set-template value)
    (seq? value) (header-seq-template value)
    ))

(defn inlined-value-template [value]
  (or (atomic-template value)
      (generic-template value)))

(defn header-template [value]
  (or (atomic-template value)
      (header-container-template value)
      (pr-str value)))

(defn build-header [value]
  (template "span" "background-color:#efe" (header-template value)))

(defn body-line-template [index value]
  (template "li" "margin-left:12px" (index-template index) (spacer) (inlined-value-template value)))

(defn body-line-templates [value]
  (let [arr #js []]
    (loop [data (seq value) ; TODO: limit max number of lines here?
           index 0]
      (if (empty? data)
        arr
        (do
          (.push arr (body-line-template index (first data)))
          (recur (rest data) (inc index)))))))

(defn build-body [value]
  (.concat (template "ol" "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px")
           (body-line-templates value)))

(defn something-abbreviated? [value]
  (if (coll? value)
    (some #(something-abbreviated? %) value)
    (= abbreviation-string value)))

(defn abbreviated? [template]
  (something-abbreviated? (js->clj template)))

(defn want-value? [value]
  (cljs-value? value))

(defn header-hook [value]
  (build-header value))

(defn has-body-hook [value]
  (abbreviated? (build-header value)))

(defn body-hook [value]
  (build-body value))

(defn call-js-fn? [target fn & args]
  (debug/log-info "passing call to original formatter")
  (if (not (nil? fn)) (.apply fn target (into-array args))))

(defn sanitize
  "wraps our hook in try-catch block to prevent leaking of exceptions if something goes wrong"
  [hook]
  (fn [value]
    (try
      (hook value)
      (catch js/Object e
        (debug/log-exception e)
        nil))))

(defn chain
  "chains our hook with original formatter"
  [name hook original-formatter]
  (fn [value]
    (if (and *devtools-enabled* (want-value? value))
      (hook value)
      (call-js-fn? original-formatter (aget original-formatter name) value))))

(defn cljs-formatter [original-formatter]
  (let [wrapper (fn [name hook] (debug/hook-monitor name (chain name (sanitize hook) original-formatter)))]
    (js-obj
      "header" (wrapper "header" header-hook)
      "hasBody" (wrapper "hasBody" has-body-hook)
      "body" (wrapper "body" body-hook))))

(defn install-devtools! []
  (set! *original-formatter* (aget js/window formatter-key))
  (aset js/window formatter-key (cljs-formatter *original-formatter*)))

; NOT SAFE
(defn uninstall-devtools! []
  (aset js/window formatter-key *original-formatter*)
  (set! *original-formatter* nil))

(defn disable-devtools! []
  (set! *devtools-enabled* false))

(defn enable-devtools! []
  (set! *devtools-enabled* true))