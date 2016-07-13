(ns devtools.format
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.munging :as munging]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [clojure.string :as string]))

(declare alt-printer-impl)
(declare build-header)

; ---------------------------------------------------------------------------------------------------------------------------
; PROTOCOL SUPPORT

(defprotocol ^:deprecated IDevtoolsFormat                                                                                     ; use IFormat instead
  (-header [value])
  (-has-body [value])
  (-body [value]))

; - state management --------------------------------------------------------------------------------------------------------
;
; we have to maintain some state:
; a) to prevent infinite recursion in some pathological cases (https://github.com/binaryage/cljs-devtools/issues/2)
; b) to keep track of printed objects to visually signal circular data structures
;
; We dynamically bind *current-config* to the config passed from "outside" when entering calls to our API methods.
; Initially the state is empty, but we accumulate there a history of seen values when rendering individual values
; in depth-first traversal order. See alt-printer-impl where we re-bind *current-config* for each traversal level.
; But there is a catch. For larger data structures our printing methods usually do not print everything at once.
; We can include so called "object references" which are just placeholders which can be expanded later
; by DevTools UI (when user clicks a disclosure triangle).
; For proper continuation in rendering of those references we have to carry our existing state over.
; We use "config" feature of custom formatters system to pass current state to future API calls.

(def ^:dynamic *current-state* nil)

(defn get-current-state []
  *current-state*)

(defn update-current-state! [f & args]
  (set! *current-state* (apply f *current-state* args)))

(defn push-object-to-current-history! [object]
  (update-current-state! update :history conj object))

(defn get-current-history []
  (:history (get-current-state)))

; ---------------------------------------------------------------------------------------------------------------------------

(defn is-prototype? [o]
  (identical? (.-prototype (.-constructor o)) o))

(defn is-js-symbol? [o]
  (= (goog/typeOf o) "symbol"))

(defn cljs-function? [value]
  (and (not (pref :disable-cljs-fn-formatting))
       (not (var? value))                                                                                                     ; HACK: vars have IFn protocol and would act as functions TODO: implement custom rendering for vars
       (munging/cljs-fn? value)))

(defn has-formatting-protocol? [value]
  (or (safe-call satisfies? false IPrintWithWriter value)
      (safe-call satisfies? false IDevtoolsFormat value)                                                                      ; legacy
      (safe-call satisfies? false IFormat value)))

; IRC #clojurescript @ freenode.net on 2015-01-27:
; [13:40:09] darwin_: Hi, what is the best way to test if I'm handled ClojureScript data value or plain javascript object?
; [14:04:34] dnolen: there is a very low level thing you can check
; [14:04:36] dnolen: https://github.com/clojure/clojurescript/blob/c2550c4fdc94178a7957497e2bfde54e5600c457/src/clj/cljs/core.clj#L901
; [14:05:00] dnolen: this property is unlikely to change - still it's probably not something anything anyone should use w/o a really good reason
(defn cljs-type? [f]
  (and (goog/isObject f)                                                                                                      ; see http://stackoverflow.com/a/22482737/84283
       (not (is-prototype? f))
       (oget f "cljs$lang$type")))

(defn cljs-instance? [value]
  (and (goog/isObject value)                                                                                                  ; see http://stackoverflow.com/a/22482737/84283
       (cljs-type? (oget value "constructor"))))

(defn cljs-land-value? [value]
  (or (cljs-instance? value)
      (has-formatting-protocol? value)))                                                                                      ; some raw js types can be extend-protocol to support cljs printing, see issue #21

(defn cljs-value? [value]
  (and
    (or (cljs-land-value? value)
        (cljs-function? value))
    (not (is-prototype? value))
    (not (is-js-symbol? value))))

(defn ^bool prevent-recursion? []
  (boolean (:prevent-recursion (get-current-state))))

(defn resolve-pref [v]
  (if (keyword? v)
    (pref v)
    v))

; -- object marking support -------------------------------------------------------------------------------------------------

(defn mark-as-group! [value]
  (specify! value IGroup)
  value)

(defn group? [value]
  (satisfies? IGroup value))

(defn mark-as-template! [value]
  (specify! value ITemplate)
  value)

(defn template? [value]
  (satisfies? ITemplate value))

(defn mark-as-surrogate! [value]
  (specify! value ISurrogate)
  value)

(defn surrogate? [value]
  (satisfies? ISurrogate value))

; ---------------------------------------------------------------------------------------------------------------------------

(defn make-group [& items]
  (let [group (mark-as-group! #js [])]
    (doseq [item items]
      (if (some? item)
        (if (coll? item)
          (.apply (aget group "push") group (mark-as-group! (into-array item)))                                               ; convenience helper to splat cljs collections
          (.push group (resolve-pref item)))))
    group))

(defn make-template
  [tag style & children]
  (let [tag (resolve-pref tag)
        style (resolve-pref style)
        template (mark-as-template! #js [tag (if (empty? style) #js {} #js {"style" style})])]
    (doseq [child children]
      (if (some? child)
        (if (coll? child)
          (.apply (aget template "push") template (mark-as-template! (into-array child)))                                     ; convenience helper to splat cljs collections
          (if-let [child-value (resolve-pref child)]
            (.push template child-value)))))
    template))

(defn concat-templates! [template & templates]
  (mark-as-template! (.apply (oget template "concat") template (into-array (map into-array templates)))))

(defn extend-template! [template & args]
  (concat-templates! template args))

(defn make-surrogate
  ([object header] (make-surrogate object header true))
  ([object header has-body] (make-surrogate object header has-body nil))
  ([object header has-body body-template]
   (mark-as-surrogate! (js-obj
                         "target" object
                         "header" header
                         "hasBody" has-body
                         "bodyTemplate" body-template))))

(defn get-target-object [value]
  (if (surrogate? value)
    (oget value "target") value))

(defn positions [pred coll]
  (keep-indexed (fn [idx x]
                  (if (pred x) idx)) coll))

(defn remove-positions [coll indices]
  (keep-indexed (fn [idx x]
                  (if-not (contains? indices idx) x)) coll))

(defn is-circular? [object]
  (let [history (get-current-history)]
    (some #(identical? % object) history)))

(defn reference? [value]
  (and (group? value)
       (= (aget value 0) "object")))

; -- TemplateWriter ---------------------------------------------------------------------------------------------------------

(deftype TemplateWriter [group]
  Object
  (merge [_ a] (.apply (.-push group) group a))
  (get-group [_] group)
  IWriter
  (-write [_ o] (.push group o))
  (-flush [_] nil))

(defn make-template-writer []
  (TemplateWriter. (make-group)))

; -- templates --------------------------------------------------------------------------------------------------------------

(defn circular-reference-template [content-group]
  (let [base-template (make-template :span :circular-reference-wrapper-style
                                     (make-template :span :circular-reference-symbol-style :circular-reference-symbol))]
    (concat-templates! base-template content-group)))

(defn nil-template [_value]
  (make-template :span :nil-style :nil-label))

(defn reference-template [object & [state-override]]
  (if (nil? object)
    (nil-template object)
    (let [sub-state (-> (get-current-state)
                        (merge state-override)
                        (update :history conj ::reference))]
      (make-group "object" #js {"object" object
                                "config" sub-state}))))

(defn native-reference-template [object]
  (reference-template object {:prevent-recursion true}))

(defn index-template [value]
  (make-template :span :index-style value :line-index-separator))

(defn number-template [value]
  (if (integer? value)
    (make-template :span :integer-style value)
    (make-template :span :float-style value)))

(defn meta-template [value]
  (let [header-template (make-template :span :meta-style "meta")
        body-template (make-template :span :meta-body-style
                                     (build-header value))]
    (make-template :span :meta-reference-style
                   (reference-template (make-surrogate value header-template true body-template)))))

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
      (make-template :span :string-style (str dq inline-string dq))
      (let [abbreviated-string-template (make-template :span :string-style (str dq (abbreviate-long-string inline-string) dq))
            string-with-nl-markers (.replace source-string re-nl (str (pref :new-line-string-replacer) "\n"))
            body-template (make-template :span :expanded-string-style string-with-nl-markers)]
        (reference-template (make-surrogate source-string abbreviated-string-template true body-template))))))

(defn cljs-function-body-template [fn-obj ns _name args prefix-template]
  (let [make-args-template (fn [args]
                             (make-template :li :aligned-li-style
                                            (make-template :span :fn-multi-arity-args-indent-style prefix-template)
                                            (make-template :span :fn-args-style args)))
        args-lists-templates (if (> (count args) 1) (map make-args-template args))
        ns-template (if-not (empty? ns)
                      (make-template :li :aligned-li-style
                                     (make-template :span :fn-ns-symbol-style :fn-ns-symbol)
                                     (make-template :span :fn-ns-name-style ns)))
        native-template (make-template :li :aligned-li-style
                                       (make-template :span :fn-native-symbol-style :fn-native-symbol)
                                       (native-reference-template fn-obj))]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  args-lists-templates
                                  ns-template
                                  native-template))))

(defn cljs-function-template [fn-obj]
  (let [[ns name] (munging/parse-fn-info fn-obj)
        args-open-symbol (pref :args-open-symbol)
        args-close-symbol (pref :args-close-symbol)
        multi-arity-symbol (pref :multi-arity-symbol)
        spacer-symbol (pref :spacer)
        rest-symbol (pref :rest-symbol)
        args-strings (munging/extract-args-strings fn-obj true spacer-symbol multi-arity-symbol rest-symbol)
        multi-arity? (> (count args-strings) 1)
        args (map #(str args-open-symbol % args-close-symbol) args-strings)
        multi-arity-marker (str args-open-symbol multi-arity-symbol args-close-symbol)
        args-template (make-template :span :fn-args-style (if multi-arity? multi-arity-marker (first args)))
        lambda? (empty? name)
        fn-name (if-not lambda?
                  (make-template :span :fn-name-style name))
        symbol-template (if lambda?
                          (make-template :span :fn-lambda-symbol-style :fn-lambda-symbol)
                          (make-template :span :fn-symbol-style :fn-symbol))
        prefix-template (make-template :span :fn-prefix-style symbol-template fn-name)
        header-template (make-template :span :fn-header-style prefix-template args-template)
        body-template (partial cljs-function-body-template fn-obj ns name args prefix-template)]
    (reference-template (make-surrogate fn-obj header-template true body-template))))

(defn present-basis [basis]
  (string/join " " (map name basis)))

(defn cljs-type-body-template [constructor-fn ns _name basis]
  (let [ns-template (if-not (empty? ns)
                      (make-template :li :aligned-li-style
                                     (make-template :span :fn-ns-symbol-style :fn-ns-symbol)
                                     (make-template :span :fn-ns-name-style ns)))
        basis-template (if-not (empty? basis)
                         (make-template :li :aligned-li-style
                                        (make-template :span :type-basis-symbol-style :type-basis-symbol)
                                        (make-template :span :type-basis-style (present-basis basis))))
        native-template (make-template :li :aligned-li-style
                                       (make-template :span :fn-native-symbol-style :fn-native-symbol)
                                       (native-reference-template constructor-fn))]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  basis-template
                                  ns-template
                                  native-template))))

(defn cljs-type-template [constructor-fn & [header-style]]
  (let [[ns name basis] (munging/parse-constructor-info constructor-fn)
        symbol-template (make-template :span :type-symbol-style :type-symbol)
        type-name-template (make-template :span :type-name-style name)
        header-template (make-template :span (or header-style :type-header-style) symbol-template type-name-template)
        body-template-fn (partial cljs-type-body-template constructor-fn ns name basis)]
    (reference-template (make-surrogate constructor-fn header-template true body-template-fn))))

(defn fetch-field [obj field]
  [field (oget obj (munge field))])

(defn fetch-instance-fields [obj basis]
  (map (partial fetch-field obj) basis))

(defn header-field-template [field]
  (let [[name value] field]
    (make-template :span :header-field-style
                   (make-template :span :header-field-name-style (str name))
                   :header-field-value-spacer
                   (make-template :span :header-field-value-style (reference-template value))
                   :header-field-separator)))

(defn body-field-template [field]
  (let [[name value] field]
    (make-template "tr" :body-field-tr-style
                   (make-template "td" :body-field-td1-style
                                  (make-template :span :body-field-symbol-style :body-field-symbol)
                                  (make-template :span :body-field-name-style (str name)))
                   (make-template "td" :body-field-td2-style
                                  :body-field-value-spacer
                                  (make-template :span :body-field-value-style (reference-template value))
                                  :header-field-value-separator))))

(defn instance-fields-header-template [fields & [max-fields]]
  (let [max-fields (or max-fields (pref :max-instance-header-fields))
        fields-templates (map header-field-template (take max-fields fields))
        more? (> (count fields) max-fields)]
    (make-template :span :fields-header-style
                   :fields-header-open-symbol
                   fields-templates
                   (if more? :more-fields-symbol)
                   :fields-header-close-symbol)))

(defn make-protocol-method-arity-template [arity-fn]
  (reference-template arity-fn))

(defn make-protocol-method-arities-list-body-template [fns]
  (let [templates (map make-protocol-method-arity-template fns)
        wrap #(make-template :li :aligned-li-style %)]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  (map wrap templates)))))

(defn make-protocol-method-arities-list-template [fns & [max-fns]]
  (let [max-fns (or max-fns (pref :max-protocol-method-arities-list))
        header-templates (map make-protocol-method-arity-template (take max-fns fns))
        more? (> (count fns) max-fns)
        template (make-template :span :protocol-method-arities-header-style
                                :protocol-method-arities-header-open-symbol
                                (interpose (pref :protocol-method-arities-list-header-separator) header-templates)
                                (if more? :protocol-method-arities-more-symbol)
                                :protocol-method-arities-header-close-symbol)]
    (if more?
      (let [body-template-fn (partial make-protocol-method-arities-list-body-template fns)]
        (reference-template (make-surrogate #js {} template true body-template-fn)))
      template)))

(defn make-protocol-method-template [[name fns]]
  (make-template :span :protocol-method-style
                 :protocol-method-symbol
                 (make-template :span :protocol-method-name-style name)
                 (make-protocol-method-arities-list-template fns)))

(defn make-protocol-body-template [obj ns _name selector _fast?]
  (let [ns-template (if-not (empty? ns)
                      (make-template :li :aligned-li-style
                                     (make-template :span :protocol-ns-symbol-style :protocol-ns-symbol)
                                     (make-template :span :protocol-ns-name-style ns)))
        protocol-obj (munging/get-protocol-object selector)
        native-template (if (some? protocol-obj)
                          (make-template :li :aligned-li-style
                                         (make-template :span :protocol-native-symbol-style :protocol-native-symbol)
                                         (native-reference-template protocol-obj)))
        methods (munging/collect-protocol-methods obj selector)
        method-templates (map make-protocol-method-template methods)
        wrap #(make-template :li :aligned-li-style %)]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  (map wrap method-templates)
                                  ns-template
                                  native-template))))

(defn make-protocol-header-template [obj protocol]
  (let [{:keys [ns name selector fast?]} protocol
        header-template (make-template :span (if fast? :header-fast-protocol-style :header-slow-protocol-style) name)
        body-template-fn (partial make-protocol-body-template obj ns name selector fast?)]
    (reference-template (make-surrogate obj header-template true body-template-fn))))

(defn make-protocols-list-body-template [obj protocols]
  (let [protocol-templates (map (partial make-protocol-header-template obj) protocols)
        wrap #(make-template :li :aligned-li-style %)]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  (map wrap protocol-templates)))))

(defn make-protocols-list-template [obj protocols & [max-protocols]]
  (let [max-protocols (or max-protocols (pref :max-list-protocols))
        protocols-header-templates (map (partial make-protocol-header-template obj) (take max-protocols protocols))
        more? (> (count protocols) max-protocols)
        template (make-template :span :protocols-header-style
                                :protocols-header-open-symbol
                                (interpose (pref :header-protocol-separator) protocols-header-templates)
                                (if more? :more-protocols-symbol)
                                :protocols-header-close-symbol)]
    (if more?
      (reference-template (make-surrogate obj template true (partial make-protocols-list-body-template obj protocols)))
      template)))

(defn instance-fields-body-template [fields obj]
  (let [protocols (munging/scan-protocols obj)
        has-protocols? (not (empty? protocols))
        protocols-list-template (if has-protocols?
                                  (make-template :li :aligned-li-style
                                                 :protocols-list-symbol
                                                 (make-protocols-list-template obj protocols)))
        native-template (make-template :li :aligned-li-style
                                       (make-template :span :fn-native-symbol-style :fn-native-symbol)
                                       (native-reference-template obj))
        fields-table-template (make-template :li :aligned-li-style
                                             (make-template "table" :instance-body-fields-table-style
                                                            (map body-field-template fields)))]
    (make-template :span :body-style
                   (make-template :ol :standard-ol-no-margin-style
                                  fields-table-template
                                  protocols-list-template
                                  native-template))))

(defn managed-print-via-protocol [value style]
  (let [tmpl (make-template :span style)
        writer (TemplateWriter. tmpl)]
    (-pr-writer value writer {:alt-impl     alt-printer-impl
                              :print-length (pref :max-header-elements)
                              :more-marker  (pref :more-marker)})
    tmpl))

(defn cljs-instance-template [value]
  (let [constructor-fn (oget value "constructor")
        [_ns _name basis] (munging/parse-constructor-info constructor-fn)
        custom-printing? (implements? IPrintWithWriter value)
        type-template (cljs-type-template constructor-fn :instance-type-header-style)
        fields (fetch-instance-fields value basis)
        fields-header-template (instance-fields-header-template fields (if custom-printing? 0))
        fields-body-template-fn #(instance-fields-body-template fields value)
        instance-value-template (make-template :span :instance-value-style
                                               (reference-template (make-surrogate value
                                                                                   fields-header-template
                                                                                   true
                                                                                   fields-body-template-fn)))
        custom-printing-template (if custom-printing?
                                   (managed-print-via-protocol value
                                                               :instance-custom-printing-style))
        header-template (make-template :span :instance-header-style
                                       type-template
                                       :instance-value-separator
                                       instance-value-template
                                       custom-printing-template)]
    (reference-template (make-surrogate value header-template false))))

(defn bool? [value]
  (or (true? value) (false? value)))

(defn bool-template [value]
  (make-template :span :bool-style value))

(defn keyword-template [value]
  (make-template :span :keyword-style (str value)))

(defn symbol-template [value]
  (make-template :span :symbol-style (str value)))

(defn instance-of-a-well-known-type? [value]
  (let [well-known-types (pref :well-known-types)
        constructor-fn (oget value "constructor")
        [ns name] (munging/parse-constructor-info constructor-fn)
        fully-qualified-type-name (str ns "/" name)]
    (contains? well-known-types fully-qualified-type-name)))

(defn atomic-template [value]
  (cond
    (nil? value) (nil-template value)
    (bool? value) (bool-template value)
    (string? value) (string-template value)
    (number? value) (number-template value)
    (keyword? value) (keyword-template value)
    (symbol? value) (symbol-template value)
    (and (cljs-instance? value) (not (instance-of-a-well-known-type? value))) (cljs-instance-template value)
    (cljs-type? value) (cljs-type-template value)
    (cljs-function? value) (cljs-function-template value)))

(defn abbreviated? [template]
  (some #(= (pref :more-marker) %) template))

(defn seq-count-is-greater-or-equal? [seq limit]
  (let [chunk (take limit seq)]                                                                                               ; we have to be extra careful to not call count on seq, it might be an infinite sequence
    (= (count chunk) limit)))

(defn expandable? [obj]
  (and
    (pref :seqables-always-expandable)
    (seqable? obj)
    (seq-count-is-greater-or-equal? obj (pref :min-sequable-count-for-expansion))))

(defn wrap-group-in-reference-if-needed [group obj]
  (if (or (expandable? obj) (abbreviated? group))
    (make-group (reference-template (make-surrogate obj (concat-templates! (make-template :span :header-style) group))))
    group))

(defn wrap-group-in-circular-warning-if-needed [group circular?]
  (if circular?
    (make-group (circular-reference-template group))
    group))

; default printer implementation can do this:
;   :else (write-all writer "#<" (str obj) ">")
; we want to wrap stringified obj in a reference for further inspection
;
; this behaviour changed in https://github.com/clojure/clojurescript/commit/34c3b8985ed8197d90f441c46d168c4024a20eb8
; newly functions and :else branch print "#object [" ... "]"
;
; in some situations obj can still be a clojurescript value (e.g. deftypes)
; we have to implement a special flag to prevent infinite recursion
; see https://github.com/binaryage/cljs-devtools/issues/2
;     https://github.com/binaryage/cljs-devtools/issues/8
(defn detect-edge-case-and-patch-it [group obj]
  (cond
    (or
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "\"]"))                                        ; function case
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "]"))                                          ; :else -constructor case
      (and (= (count group) 3) (= (aget group 0) "#object[") (= (aget group 2) "]")))                                         ; :else -cljs$lang$ctorStr case
    (make-group (native-reference-template obj))

    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
    (make-group (aget group 0) (native-reference-template obj) (aget group 2))

    :else group))

(defn wrap-value-as-reference-if-needed [value]
  (if (or (string? value) (template? value) (group? value))
    value
    (reference-template value)))

(defn wrap-group-values-as-references-if-needed [group]
  (let [result (make-group)]
    (doseq [value group]
      (.push result (wrap-value-as-reference-if-needed value)))
    result))

(defn alt-printer-job [obj writer opts]
  (if (or (safe-call satisfies? false IDevtoolsFormat obj)
          (safe-call satisfies? false IFormat obj))                                                                           ; we have to wrap value in reference if detected IFormat
    (-write writer (reference-template obj))
    (if-let [tmpl (atomic-template obj)]
      (-write writer tmpl)
      (let [default-impl (:fallback-impl opts)
            ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
            inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
        (default-impl obj writer inner-opts)))))

(defn post-process-printed-output [output-group obj circular?]
  (-> output-group
      (detect-edge-case-and-patch-it obj)                                                                                     ; an ugly hack
      (wrap-group-values-as-references-if-needed)                                                                             ; issue #21
      (wrap-group-in-reference-if-needed obj)
      (wrap-group-in-circular-warning-if-needed circular?)))

(defn alt-printer-impl [obj writer opts]
  (binding [*current-state* (get-current-state)]
    (let [circular? (is-circular? obj)
          inner-writer (make-template-writer)]
      (push-object-to-current-history! obj)
      (alt-printer-job obj inner-writer opts)
      (.merge writer (post-process-printed-output (.get-group inner-writer) obj circular?)))))

(defn managed-pr-str [value style print-level]
  (let [tmpl (make-template :span style)
        writer (TemplateWriter. tmpl)]
    (binding [*print-level* (inc print-level)]                                                                                ; when printing do at most print-level deep recursion
      (pr-seq-writer [value] writer {:alt-impl     alt-printer-impl
                                     :print-length (pref :max-header-elements)
                                     :more-marker  (pref :more-marker)}))
    tmpl))

(defn build-header [value]
  (let [value-template (managed-pr-str value :header-style (pref :max-print-level))]
    (if-let [meta-data (if (pref :print-meta-data) (meta value))]
      (make-template :span :meta-wrapper-style value-template (meta-template meta-data))
      value-template)))

(defn build-header-wrapped [value]
  (make-template :span :cljs-style (build-header value)))

(defn standard-body-template
  ([lines] (standard-body-template lines true))
  ([lines margin?] (let [ol-style (if margin? :standard-ol-style :standard-ol-no-margin-style)
                         li-style (if margin? :standard-li-style :standard-li-no-margin-style)]
                     (make-template :ol ol-style (map #(make-template :li li-style %) lines)))))

(defn body-line-template [index value]
  [(index-template index) (managed-pr-str value :item-style (pref :body-line-max-print-level))])

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
      (let [more-label-template (make-template :span :body-items-more-label-style (pref :body-items-more-label))
            surrogate-object (make-surrogate rest more-label-template)]
        (aset surrogate-object "startingIndex" (+ starting-index max-number-body-items))
        (conj lines (reference-template surrogate-object))))))

(defn build-body [value starting-index]
  (let [is-body? (zero? starting-index)
        result (standard-body-template (body-lines-templates value starting-index) is-body?)]
    (if is-body?
      (make-template :span :body-style result)
      result)))

(defn standard-reference [target]
  (make-template :ol :standard-ol-style (make-template :li :standard-li-style (reference-template target))))

(defn build-surrogate-body [value]
  (if-let [body-template (aget value "bodyTemplate")]
    (if (fn? body-template)
      (body-template)
      body-template)
    (let [target (aget value "target")]
      (if (seqable? target)
        (let [starting-index (or (aget value "startingIndex") 0)]
          (build-body target starting-index))
        (standard-reference target)))))

; ---------------------------------------------------------------------------------------------------------------------------
; RAW API

(defn want-value?* [value]
  (cond
    (prevent-recursion?) false                                                                                                ; the value won't be rendered by our custom formatter
    :else (or (cljs-value? value) (surrogate? value))))

(defn header* [value]
  (cond
    (surrogate? value) (aget value "header")
    (safe-call satisfies? false IDevtoolsFormat value) (-header value)
    (safe-call satisfies? false IFormat value) (devtools.protocols/-header value)
    :else (build-header-wrapped value)))

(defn has-body* [value]
  ; note: body is emulated using surrogate references
  (cond
    (surrogate? value) (aget value "hasBody")
    (safe-call satisfies? false IDevtoolsFormat value) (-has-body value)
    (safe-call satisfies? false IFormat value) (devtools.protocols/-has-body value)
    :else false))

(defn body* [value]
  (cond
    (surrogate? value) (build-surrogate-body value)
    (safe-call satisfies? false IDevtoolsFormat value) (-body value)
    (safe-call satisfies? false IFormat value) (devtools.protocols/-body value)))

; ---------------------------------------------------------------------------------------------------------------------------
; RAW API config-aware, see state management documentation above

(defn config-wrapper [raw-fn]
  (fn [value config]
    (binding [*current-state* (or config {})]
      (raw-fn value))))

(def want-value? (config-wrapper want-value?*))
(def header (config-wrapper header*))
(def has-body (config-wrapper has-body*))
(def body (config-wrapper body*))

; ---------------------------------------------------------------------------------------------------------------------------
; API CALLS

(defn wrap-with-exception-guard [f]
  (fn [& args]
    (try
      (apply f args)
      (catch :default e
        (.error js/console "CLJS DevTools internal error:" e)
        nil))))

(defn build-api-call [raw-fn pre-handler-key post-handler-key]
  "Wraps raw API call in a function which calls pre-handler and post-handler.

pre-handler gets a chance to pre-process value before it is passed to cljs-devtools
post-handler gets a chance to post-process value returned by cljs-devtools."
  (let [handler (fn [value config]
                  (let [pre-handler (or (pref pre-handler-key) identity)
                        post-handler (or (pref post-handler-key) identity)
                        preprocessed-value (pre-handler value)
                        result (if (want-value? preprocessed-value config)
                                 (raw-fn preprocessed-value config))]
                    (post-handler result)))]
    (wrap-with-exception-guard handler)))

(def header-api-call (build-api-call header :header-pre-handler :header-post-handler))
(def has-body-api-call (build-api-call has-body :has-body-pre-handler :has-body-post-handler))
(def body-api-call (build-api-call body :body-pre-handler :body-post-handler))
