(ns devtools.core
  (:require [devtools.debug :as debug]))

(def max-coll-elements 10)
(def max-map-elements 5)
(def max-set-elements 7)
(def max-seq-elements 20)
(def abbreviation "…")
(def line-index-separator ":")
(def formatter-key "devtoolsFormatter")
(def dq "\"")

(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *devtools-installed* false)
(def ^:dynamic *original-formatter* nil)

(declare inlined-value-template)

; dirty
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "_hash"))))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn surrogate? [value]
  (exists? (aget value "__surrogate")))

(defn template [tag style & more]
  (let [arr #js [tag (if (empty? style) #js {} #js {"style" style})]]
    (doseq [o more]
      (if (seq? o)
        (.apply (aget arr "push") arr (into-array o)) ; convenience helper to splat cljs-seqs
        (.push arr o)))
    arr))

(defn reference [object & more]
  (let [arr #js ["object" #js {"object" object}]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn surrogate [object header]
  (js-obj
    "__surrogate" true
    "target" object
    "header" header))

(defn spacer [& _] " ")

(defn nil-template [_]
  (template "span" "color:#808080" "nil"))

(defn keyword-template [value]
  (template "span" "color:#881391" (str ":" (name value))))

(defn symbol-template [value]
  (template "span" "color:#000000" (str value)))

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
  (template "span" "color:#C41A16" (str dq value dq)))

(defn fn-template [value]
  (template "span" "color:#f0f" (reference (surrogate value "λ"))))

(defn header-inlined-templates [value renderer max]
  (let [rendered-items (apply concat (interpose [(spacer)] (map renderer (take max value))))]
    (if (> (count value) max)
      (concat rendered-items [abbreviation])
      rendered-items)))

(defn header-map-template [value]
  (let [renderer (fn [[key value]] [(inlined-value-template key) (spacer) (inlined-value-template value)])
        items (header-inlined-templates value renderer max-map-elements)]
    (template "span" "color:#000" "{" items "}")))

(defn header-set-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-set-elements)]
    (template "span" "color:#000" "#{" items "}")))

(defn header-seq-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-seq-elements)]
    (template "span" "color:#000" "(" items ")")))

(defn header-coll-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-coll-elements)]
    (template "span" "color:#000" "[" items "]")))

(defn bool-template [value]
  (template "span" "color:#099" value))

(defn generic-template [value]
  (template "span" "color:#000" (reference value)))

(defn js-object-template [value]
  (if (js-value? value)
    (template "span" "color:#000" (reference (surrogate value "#js")))))

(defn bool? [value]
  (or (true? value) (false? value)))

(defn atomic-template [value]
  (cond
    (nil? value) (nil-template value)
    (bool? value) (bool-template value)
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
    (coll? value) (header-coll-template value)
    ))

(defn inlined-value-template [value]
  (or (atomic-template value)
      (js-object-template value)
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
    (= abbreviation value)))

(defn abbreviated? [template]
  (something-abbreviated? (js->clj template)))

(defn want-value? [value]
  (or (cljs-value? value)
      (surrogate? value)))

(defn header-hook [value]
  (if (surrogate? value)
    (.-header value)
    (build-header value)))

(defn has-body-hook [value]
  (if (surrogate? value)
    true
    (abbreviated? (build-header value))))

(defn build-surrogate-body [value]
  (let [target (.-target value)]
    (template "ol"
              "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px"
              (template "li" "margin-left:12px" (reference target (pr-str target))))))

(defn body-hook [value]
  (if (surrogate? value)
    (build-surrogate-body value)
    (build-body value)))

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
  (let [call-original-formatter? (fn [value]
                                   (if (not (nil? original-formatter))
                                     (do ; TODO should we wrap this in try-catch instead?
                                       (debug/log-info "passing call to original formatter")
                                       (.call (aget original-formatter name) original-formatter value))))]
    (fn [value]
      (if (and *devtools-enabled* (want-value? value))
        (hook value)
        (call-original-formatter? value)))))

(defn cljs-formatter [original-formatter]
  (let [wrapper (fn [name hook] (debug/hook-monitor name (chain name (sanitize hook) original-formatter)))]
    (js-obj
      "header" (wrapper "header" header-hook)
      "hasBody" (wrapper "hasBody" has-body-hook)
      "body" (wrapper "body" body-hook))))

(defn install-devtools! []
  (if *devtools-installed*
    (debug/log-info "devtools already installed - nothing to do")
    (do
      (set! *devtools-installed* true)
      (set! *original-formatter* (aget js/window formatter-key))
      (aset js/window formatter-key (cljs-formatter *original-formatter*)))))

; NOT SAFE
(defn uninstall-devtools! []
  (aset js/window formatter-key *original-formatter*)
  (set! *original-formatter* nil)
  (set! *devtools-installed* false))

(defn disable-devtools! []
  (set! *devtools-enabled* false))

(defn enable-devtools! []
  (set! *devtools-enabled* true))