(ns devtools.format
  (:require [devtools.prefs :refer [pref]]))

; IRC #clojurescript @ freenode.net on 2015-01-27:
; [13:40:09] darwin_: Hi, what is the best way to test if I'm handled ClojureScript data value or plain javascript object?
; [14:04:34] dnolen: there is a very low level thing you can check
; [14:04:36] dnolen: https://github.com/clojure/clojurescript/blob/c2550c4fdc94178a7957497e2bfde54e5600c457/src/clj/cljs/core.clj#L901
; [14:05:00] dnolen: this property is unlikely to change - still it's probably not something anything anyone should use w/o a really good reason
(defn cljs-value? [value]
  (try
    (.. value -constructor -cljs$lang$type)
    (catch :default _
      false)))

(defn surrogate? [value]
  (and
    (not (nil? value))
    (aget value (pref :surrogate-key))))

(defn prevent-recursion? [config]
  (and
    (not (nil? config))
    (aget config "prevent-recursion")))

(defn template [tag style & children]
  (let [js-array #js [tag (if (empty? style) #js {} #js {"style" style})]]
    (doseq [child children]
      (if (coll? child)
        (.apply (aget js-array "push") js-array (into-array child)) ; convenience helper to splat cljs collections
        (.push js-array child)))
    js-array))

(defn reference
  ([object] #js ["object" #js {"object" object}])
  ([object config] #js ["object" #js {"object" object "config" config}]))

(defn surrogate
  ([object header] (surrogate object header true))
  ([object header has-body] (surrogate object header has-body nil))
  ([object header has-body body-template]
   (js-obj
     (pref :surrogate-key) true
     "target" object
     "header" (template (pref :span) (pref :cljs-style) header)
     "hasBody" has-body
     "bodyTemplate" body-template)))

(defn index-template [value]
  (template (pref :span) (pref :index-style) value (pref :line-index-separator)))

(defn number-template [value]
  (if (integer? value)
    (template (pref :span) (pref :integer-style) value)
    (template (pref :span) (pref :float-style) value)))

(defn abbreviate-long-string [string]
  (str
    (apply str (take (pref :string-prefix-limit) string))
    (pref :string-abbreviation-marker)
    (apply str (take-last (pref :string-postfix-limit) string))))

(defn string-template [source-string]
  (let [dq (pref :dq)
        re-nl (js/RegExp. "\n" "g")
        inline-string (.replace source-string re-nl (pref :new-line-string-replacer))
        max-inline-string-size (+ (pref :string-prefix-limit) (pref :string-postfix-limit))]
    (if (<= (count inline-string) max-inline-string-size)
      (template (pref :span) (pref :string-style) (str dq inline-string dq))
      (let [abbreviated-string-template (template (pref :span) (pref :string-style) (str dq (abbreviate-long-string inline-string) dq))
            string-with-nl-markers (.replace source-string re-nl (str (pref :new-line-string-replacer) "\n"))
            body-template (template (pref :ol) (pref :standard-ol-style)
                            (template (pref :li) (pref :standard-li-style)
                              (template (pref :span) (pref :string-style) (str dq string-with-nl-markers dq))))]
        (reference (surrogate source-string abbreviated-string-template true body-template))))))

(defn bool? [value]
  (or (true? value) (false? value)))

(defn atomic-template [value]
  (cond
    (nil? value) (template (pref :span) (pref :nil-style) (pref :nil-label))
    (bool? value) (template (pref :span) (pref :bool-style) value)
    (string? value) (string-template value)
    (number? value) (number-template value)
    (keyword? value) (template (pref :span) (pref :keyword-style) (str value))
    (symbol? value) (template (pref :span) (pref :symbol-style) (str value))
    (fn? value) (template (pref :span) (pref :fn-style) (reference value))))

(defn abbreviated? [template]
  (some #(= (pref :more-marker) %) template))

(deftype TemplateWriter [t]
  Object
  (merge [_ a] (.apply (.-push t) t a))
  IWriter
  (-write [_ o] (.push t o))
  (-flush [_] nil))

(defn wrap-group-in-reference-if-needed [group obj]
  (if (abbreviated? group)
    #js [(reference (surrogate obj (.concat (template (pref :span) "") group)))]
    group))

; default printer implementation can do this:
;   :else (write-all writer "#<" (str obj) ">")
; we want to wrap stringified obj in a reference for further inspection
;
; in some situations obj can still be a clojurescript value (e.g. deftypes)
; we have to implement a special flag to prevent infinite recursion
; see https://github.com/binaryage/cljs-devtools/issues/2
(defn detect-else-case-and-patch-it [group obj]
  (if (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))
    (aset group 1 (reference obj #js {"prevent-recursion" true}))))

(defn alt-printer-impl [obj writer opts]
  (if-let [tmpl (atomic-template obj)]
    (-write writer tmpl)
    (let [inner-tmpl #js []
          inner-writer (TemplateWriter. inner-tmpl)
          default-impl (:fallback-impl opts)
          ; we want to (pref :li)mit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
          inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
      (default-impl obj inner-writer inner-opts)
      (detect-else-case-and-patch-it inner-tmpl obj)        ; an ugly special case
      (.merge writer (wrap-group-in-reference-if-needed inner-tmpl obj) obj))))

(defn managed-pr-str [value style print-level]
  (let [tmpl (template (pref :span) style)
        writer (TemplateWriter. tmpl)]
    (binding [*print-level* print-level]                    ; when printing do at most print-level deep recursion
      (pr-seq-writer [value] writer {:alt-impl     alt-printer-impl
                                     :print-length (pref :max-header-elements)
                                     :more-marker  (pref :more-marker)}))
    tmpl))

(defn build-header [value]
  (managed-pr-str value (pref :cljs-style) (inc (pref :max-print-level))))

(defn standard-body-template
  ([lines margin?] (let [ol-style (if margin? (pref :standard-ol-style) (pref :standard-ol-no-margin-style))
                         li-style (if margin? (pref :standard-li-style) (pref :standard-li-no-margin-style))]
                     (template (pref :ol) ol-style (map #(template (pref :li) li-style %) lines))))
  ([lines] (standard-body-template lines true)))

(defn body-line-template [index value]
  [(index-template index) (pref :spacer) (managed-pr-str value (if cljs-value? (pref :cljs-style) "") 3)])

(defn prepare-body-lines [data starting-index]
  (loop [work data
         index starting-index
         lines []]
    (if (empty? work)
      lines
      (recur (rest work) (inc index) (conj lines (body-line-template index (first work)))))))

(defn body-lines-templates [value starting-index]
  (let [seq (seq value)
        max-number-body-items (pref :max-number-body-items)
        chunk (take max-number-body-items seq)
        rest (drop max-number-body-items seq)
        lines (prepare-body-lines chunk starting-index)
        continue? (not (empty? (take 1 rest)))]
    (if-not continue?
      lines
      (let [surrogate-object (surrogate rest (pref :body-items-more-label))]
        (aset surrogate-object "startingIndex" (+ starting-index max-number-body-items))
        (conj lines (reference surrogate-object))))))

(defn build-body [value starting-index]
  (standard-body-template (body-lines-templates value starting-index) (zero? starting-index)))

(defn build-surrogate-body [value]
  (if-let [body-template (aget value "bodyTemplate")]
    body-template
    (let [target (aget value "target")]
      (if (seqable? target)
        (let [starting-index (or (aget value "startingIndex") 0)]
          (build-body target starting-index))
        (template (pref :ol) (pref :standard-ol-style) (template (pref :li) (pref :standard-li-style) (reference target)))))))

;;;;;;;;; PROTOCOL SUPPORT

(defprotocol IDevtoolsFormat
  (-header [value])
  (-has-body [value])
  (-body [value]))

;;;;;;;;; RAW API

(defn want-value? [value config]
  (if (prevent-recursion? config)
    false
    (or (cljs-value? value) (surrogate? value))))

(defn header [value config]
  (cond
    (surrogate? value) (aget value "header")
    (satisfies? IDevtoolsFormat value) (-header value)
    :else (build-header value)))

(defn has-body [value config]
  ; note: body is emulated using surrogate references
  (cond
    (surrogate? value) (aget value "hasBody")
    (satisfies? IDevtoolsFormat value) (-has-body value)
    :else false))

(defn body [value config]
  (cond
    (surrogate? value) (build-surrogate-body value)
    (satisfies? IDevtoolsFormat value) (-body value)))

;;;;;;;;; API CALLS

(defn header-api-call [value config]
  (if (want-value? value config)
    (header value config)))

(defn has-body-api-call [value config]
  (if (want-value? value config)
    (has-body value config)
    false))

(defn body-api-call [value config]
  (if (want-value? value config)
    (body value config)))