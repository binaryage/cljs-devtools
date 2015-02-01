(ns devtools.format)

(def max-coll-elements 10)
(def max-map-elements 5)
(def max-set-elements 10)
(def max-seq-elements 20)
(def abbreviation "…")
(def line-index-separator ":")
(def dq "\"")
(def surrogate-key "$$surrogate")
(def standard-ol-style "list-style-type:none; padding-left:0px; margin-top:0px; margin-bottom:0px; margin-left:12px")
(def standard-li-style "margin-left:12px")
(def spacer " ")
(def span "span")
(def ol "ol")
(def li "li")

(declare inlined-value-template)

; dirty TODO: find a reliable way how to detect cljs values
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "__meta"))
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

(defn surrogate
  ([object header] (surrogate object header true))
  ([object header has-body] (js-obj
                              surrogate-key true
                              "target" object
                              "header" header
                              "hasBody" has-body)))

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
    :else (pr-str value)))                                  ; TODO: should we handle IDelay and others? I believe it is not safe to dereference them here

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

(defn standard-body-template [lines]
  (template ol standard-ol-style (map #(template li standard-li-style %) lines)))

(defn body-line-template [index value]
  [(index-template index) spacer (inlined-value-template value)])

(defn body-lines-templates [value]
  (loop [data (seq value)                                   ; TODO: limit max number of lines here?
         index 0
         lines []]
    (if (empty? data)
      lines
      (recur (rest data) (inc index) (conj lines (body-line-template index (first data)))))))

(defn build-body [value]
  (standard-body-template (body-lines-templates value)))

(defn something-abbreviated? [value]
  (if (coll? value)
    (some #(something-abbreviated? %) value)
    (= abbreviation value)))

(defn abbreviated? [template]
  (something-abbreviated? (js->clj template)))

(defn build-surrogate-body [value]
  (let [target (.-target value)]
    (template ol standard-ol-style (template li standard-li-style (reference target (pr-str target))))))

(defn want-value? [value]
  (or (cljs-value? value)
      (surrogate? value)))

;;;;;;;;; PROTOCOL SUPPORT

(defprotocol IDevtoolsFormat
  (-header [value])
  (-has-body [value])
  (-body [value]))

;;;;;;;;; API CALLS

(defn header-api-call [value]
  (if (surrogate? value)
    (.-header value)
    (if (satisfies? IDevtoolsFormat value)
      (-header value)
      (build-header value))))

(defn has-body-api-call [value]
  (if (surrogate? value)
    (.-hasBody value)
    (if (satisfies? IDevtoolsFormat value)
      (-has-body value)
      (abbreviated? (build-header value)))))

(defn body-api-call [value]
  (if (surrogate? value)
    (build-surrogate-body value)
    (if (satisfies? IDevtoolsFormat value)
      (-body value)
      (build-body value))))