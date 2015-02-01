(ns devtools.core
  (:require [devtools.debug :as debug]))

(def max-coll-elements 10)
(def max-map-elements 5)
(def max-set-elements 10)
(def max-seq-elements 20)
(def abbreviation "…")
(def line-index-separator ":")
(def formatter-key "devtoolsFormatter")
(def dq "\"")
(def surrogate-key "$$surrogate")
(def standard-ol-style "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px")
(def standard-li-style "margin-left:12px")
(def spacer " ")
(def span "span")
(def ol "ol")
(def li "li")

(def ^:dynamic *devtools-installed* false)
(def ^:dynamic *devtools-enabled* true)
(def ^:dynamic *original-formatter* nil)
(def ^:dynamic *sanitizer-enabled* true)

(declare inlined-value-template)

; dirty TODO: find a reliable way how to detect cljs values
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "_hash"))))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn surrogate? [value]
  (exists? (aget value surrogate-key)))

(defn template [tag style & children]
  (let [js-array #js [tag (if (empty? style) #js {} #js {"style" style})]]
    (doseq [child children]
      (if (coll? child)
        (.apply (aget js-array "push") js-array (into-array child)) ; convenience helper to splat cljs collections
        (.push js-array child)))
    js-array))

(defn reference [object & children]
  (let [js-array #js ["object" #js {"object" object}]]
    (doseq [child children]
      (.push js-array child))
    js-array))

(defn surrogate [object header]
  (js-obj
    surrogate-key true
    "target" object
    "header" header))

(defn nil-template [_]
  (template span "color:#808080" "nil"))

(defn keyword-template [value]
  (template span "color:#881391" (str ":" (name value))))

(defn symbol-template [value]
  (template span "color:#000000" (str value)))

(defn number-template [value]
  (if (integer? value)
    (template span "color:#1C00CF" value)
    (template span "color:#1C88CF" value)))

(defn index-template [value]
  (template span "color:#881391" value line-index-separator))

(defn deref-template [value]
  (cond
    (satisfies? IAtom value) (template span "color:#f0f" "#<Atom " (reference @value) ">")
    (satisfies? IVolatile value) (template span "color:#f0f" "#<Volatile " (reference @value) ">")
    :else (pr-str value))) ; TODO: should we handle IDelay and others? I believe it is not safe to dereference them here

; TODO: abbreviate long strings
(defn string-template [value]
  (template span "color:#C41A16" (str dq value dq)))

(defn fn-template [value]
  (template span "color:#090" (reference (surrogate value "λ"))))

(defn header-inlined-templates [value renderer max]
  (let [rendered-items (apply concat (interpose [spacer] (map renderer (take max value))))]
    (if (> (count value) max)
      (concat rendered-items [abbreviation])
      rendered-items)))

(defn header-map-template [value]
  (let [renderer (fn [[key value]] [(inlined-value-template key) spacer (inlined-value-template value)])
        items (header-inlined-templates value renderer max-map-elements)]
    (template span "color:#000" "{" items "}")))

(defn header-set-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-set-elements)]
    (template span "color:#000" "#{" items "}")))

(defn header-seq-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-seq-elements)]
    (template span "color:#000" "(" items ")")))

(defn header-coll-template [value]
  (let [renderer (fn [item] [(inlined-value-template item)])
        items (header-inlined-templates value renderer max-coll-elements)]
    (template span "color:#000" "[" items "]")))

(defn bool-template [value]
  (template span "color:#099" value))

(defn generic-template [value]
  (template span "color:#000" (reference value)))

(defn js-object-template [value]
  (if (js-value? value)
    (template span "color:#000" (reference (surrogate value "#js"))))) ; TODO: we could render short preview of #js value here

(defn bool? [value]
  (or (true? value) (false? value)))

(defn deref? [value]
  (satisfies? IDeref value))

(defn atomic-template [value]
  (cond
    (nil? value) (nil-template value)
    (bool? value) (bool-template value)
    (string? value) (string-template value)
    (number? value) (number-template value)
    (keyword? value) (keyword-template value)
    (symbol? value) (symbol-template value)
    (fn? value) (fn-template value)
    (deref? value) (deref-template value)))

(defn header-container-template [value]
  (cond
    (map? value) (header-map-template value)
    (set? value) (header-set-template value)
    (seq? value) (header-seq-template value)
    (coll? value) (header-coll-template value)))

(defn inlined-value-template [value]
  (or (atomic-template value)
      (js-object-template value)
      (generic-template value)))

(defn header-template [value]
  (or (atomic-template value)
      (header-container-template value)
      (pr-str value)))

(defn build-header [value]
  (template span "background-color:#efe" (header-template value)))

(defn body-line-template [index value]
  (template li standard-li-style (index-template index) spacer (inlined-value-template value)))

(defn body-line-templates [value]
  (loop [data (seq value) ; TODO: limit max number of lines here?
         index 0
         lines []]
    (if (empty? data)
      lines
      (recur (rest data) (inc index) (conj lines (body-line-template index (first data)))))))

(defn build-body [value]
  (template ol standard-ol-style (body-line-templates value)))

(defn something-abbreviated? [value]
  (if (coll? value)
    (some #(something-abbreviated? %) value)
    (= abbreviation value)))

(defn abbreviated? [template]
  (something-abbreviated? (js->clj template)))

(defn want-value? [value]
  (or (cljs-value? value)
      (surrogate? value)))

(defn build-surrogate-body [value]
  (let [target (.-target value)]
    (template ol standard-ol-style (template li standard-li-style (reference target (pr-str target))))))

(defn header-hook [value]
  (if (surrogate? value)
    (.-header value)
    (build-header value)))

(defn has-body-hook [value]
  (if (surrogate? value)
    true
    (abbreviated? (build-header value))))

(defn body-hook [value]
  (if (surrogate? value)
    (build-surrogate-body value)
    (build-body value)))

(def api-mapping [["header" header-hook]
                  ["hasBody" has-body-hook]
                  ["body" body-hook]])

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
  [name original-formatter enabled? hook]
  (let [call-original-formatter? (fn [value]
                                   (if (not (nil? original-formatter))
                                     (do ; TODO should we wrap this in try-catch instead?
                                       (debug/log-info "passing call to original formatter")
                                       (.call (aget original-formatter name) original-formatter value))))]
    (fn [value]
      (if (and (enabled?) (want-value? value))
        (hook value)
        (call-original-formatter? value)))))

(defn cljs-formatter [enabler-pred original-formatter sanitizer-enabled]
  (let [hook-wrapper (fn [name hook]
                       (let [monitor (partial debug/hook-monitor name)
                             chainer (partial chain name original-formatter enabler-pred)
                             sanitizer (if sanitizer-enabled sanitize identity)]
                         ((comp monitor chainer sanitizer) hook)))
        api-gen (fn [[name hook]] [name (hook-wrapper name hook)])]
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