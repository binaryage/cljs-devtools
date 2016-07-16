(ns devtools.formatters.core
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.format :refer [IDevtoolsFormat]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.templating :refer [make-template make-group make-surrogate concat-templates! extend-template!
                                                    get-target-object
                                                    group? template? surrogate? render-json-ml
                                                    render-markup]]
            [devtools.formatters.helpers :refer [cljs-value?]]
            [devtools.formatters.state :refer [prevent-recursion? *current-state* get-current-state]]
            [devtools.formatters.markup :as markup]))

;(declare alt-printer-impl)
;(declare build-header)
;
;; - state management --------------------------------------------------------------------------------------------------------
;;
;; we have to maintain some state:
;; a) to prevent infinite recursion in some pathological cases (https://github.com/binaryage/cljs-devtools/issues/2)
;; b) to keep track of printed objects to visually signal circular data structures
;;
;; We dynamically bind *current-config* to the config passed from "outside" when entering calls to our API methods.
;; Initially the state is empty, but we accumulate there a history of seen values when rendering individual values
;; in depth-first traversal order. See alt-printer-impl where we re-bind *current-config* for each traversal level.
;; But there is a catch. For larger data structures our printing methods usually do not print everything at once.
;; We can include so called "object references" which are just placeholders which can be expanded later
;; by DevTools UI (when user clicks a disclosure triangle).
;; For proper continuation in rendering of those references we have to carry our existing state over.
;; We use "config" feature of custom formatters system to pass current state to future API calls.
;
;(def ^:dynamic *current-state* nil)
;
;(defn get-current-state []
;  *current-state*)
;
;(defn update-current-state! [f & args]
;  (set! *current-state* (apply f *current-state* args)))
;
;(defn push-object-to-current-history! [object]
;  (update-current-state! update :history conj object))
;
;(defn get-current-history []
;  (:history (get-current-state)))

; ---------------------------------------------------------------------------------------------------------------------------

;(defn is-prototype? [o]
;  (identical? (.-prototype (.-constructor o)) o))
;
;(defn is-js-symbol? [o]
;  (= (goog/typeOf o) "symbol"))
;
;(defn cljs-function? [value]
;  (and (not (pref :disable-cljs-fn-formatting))
;       (not (var? value))                                                                                                     ; HACK: vars have IFn protocol and would act as functions TODO: implement custom rendering for vars
;       (munging/cljs-fn? value)))
;
;(defn has-formatting-protocol? [value]
;  (or (safe-call satisfies? false IPrintWithWriter value)
;      (safe-call satisfies? false IDevtoolsFormat value)                                                                      ; legacy
;      (safe-call satisfies? false IFormat value)))
;
;; IRC #clojurescript @ freenode.net on 2015-01-27:
;; [13:40:09] darwin_: Hi, what is the best way to test if I'm handled ClojureScript data value or plain javascript object?
;; [14:04:34] dnolen: there is a very low level thing you can check
;; [14:04:36] dnolen: https://github.com/clojure/clojurescript/blob/c2550c4fdc94178a7957497e2bfde54e5600c457/src/clj/cljs/core.clj#L901
;; [14:05:00] dnolen: this property is unlikely to change - still it's probably not something anything anyone should use w/o a really good reason
;(defn cljs-type? [f]
;  (and (goog/isObject f)                                                                                                      ; see http://stackoverflow.com/a/22482737/84283
;       (not (is-prototype? f))
;       (oget f "cljs$lang$type")))
;
;(defn cljs-instance? [value]
;  (and (goog/isObject value)                                                                                                  ; see http://stackoverflow.com/a/22482737/84283
;       (cljs-type? (oget value "constructor"))))
;
;(defn cljs-land-value? [value]
;  (or (cljs-instance? value)
;      (has-formatting-protocol? value)))                                                                                      ; some raw js types can be extend-protocol to support cljs printing, see issue #21
;
;(defn cljs-value? [value]
;  (and
;    (or (cljs-land-value? value)
;        (cljs-function? value))
;    (not (is-prototype? value))
;    (not (is-js-symbol? value))))
;
;(defn ^bool prevent-recursion? []
;  (boolean (:prevent-recursion (get-current-state))))
;
;(defn resolve-pref [v]
;  (if (keyword? v)
;    (pref v)
;    v))
;
;(defn positions [pred coll]
;  (keep-indexed (fn [idx x]
;                  (if (pred x) idx)) coll))
;
;(defn remove-positions [coll indices]
;  (keep-indexed (fn [idx x]
;                  (if-not (contains? indices idx) x)) coll))
;
;(defn is-circular? [object]
;  (let [history (get-current-history)]
;    (some #(identical? % object) history)))
;
;(defn reference? [value]
;  (and (group? value)
;       (= (aget value 0) "object")))
;
;(defn reference-template [object & [state-override]]
;  (if (nil? object)
;    (render-json-ml (markup/<nil>))
;    (let [sub-state (-> (get-current-state)
;                        (merge state-override)
;                        #_(update :history conj ::reference))]
;      (make-group "object" #js {"object" object
;                                "config" sub-state}))))

; -- templates --------------------------------------------------------------------------------------------------------------

;(defn cljs-function-body-template [fn-obj ns _name args prefix-template]
;  (let [make-args-template (fn [args]
;                             (make-template :li :aligned-li-style
;                                            (make-template :span :fn-multi-arity-args-indent-style prefix-template)
;                                            (make-template :span :fn-args-style args)))
;        args-lists-templates (if (> (count args) 1) (map make-args-template args))
;        ns-template (if-not (empty? ns)
;                      (make-template :li :aligned-li-style
;                                     :ns-icon
;                                     (make-template :span :fn-ns-name-style ns)))
;        native-template (make-template :li :aligned-li-style
;                                       :native-icon
;                                       (render-json-ml (markup/native-reference fn-obj)))]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  args-lists-templates
;                                  ns-template
;                                  native-template))))
;
;(defn cljs-function-template [fn-obj]
;  (let [[ns name] (munging/parse-fn-info fn-obj)
;        args-open-symbol (pref :args-open-symbol)
;        args-close-symbol (pref :args-close-symbol)
;        multi-arity-symbol (pref :multi-arity-symbol)
;        spacer-symbol (pref :spacer)
;        rest-symbol (pref :rest-symbol)
;        args-strings (munging/extract-args-strings fn-obj true spacer-symbol multi-arity-symbol rest-symbol)
;        multi-arity? (> (count args-strings) 1)
;        args (map #(str args-open-symbol % args-close-symbol) args-strings)
;        multi-arity-marker (str args-open-symbol multi-arity-symbol args-close-symbol)
;        args-template (make-template :span :fn-args-style (if multi-arity? multi-arity-marker (first args)))
;        lambda? (empty? name)
;        fn-name (if-not lambda? (make-template :span :fn-name-style name))
;        symbol-template (if lambda? :lambda-icon :fn-icon)
;        prefix-template (make-template :span :fn-prefix-style symbol-template fn-name)
;        header-template (make-template :span :fn-header-style prefix-template args-template)
;        body-template (partial cljs-function-body-template fn-obj ns name args prefix-template)]
;    (reference-template (make-surrogate fn-obj header-template true body-template))))
;
;(defn make-basis-template [basis]
;  (make-template :span :type-basis-style
;                 (string/join " " (map name basis))))
;
;(defn cljs-type-body-template [constructor-fn ns _name basis]
;  (let [ns-template (if-not (empty? ns)
;                      (make-template :li :aligned-li-style
;                                     :ns-icon
;                                     (make-template :span :fn-ns-name-style ns)))
;        basis-template (if-not (empty? basis)
;                         (make-template :li :aligned-li-style
;                                        :basis-icon
;                                        (make-basis-template basis)))
;        native-template (make-template :li :aligned-li-style
;                                       (make-template :span :fn-native-symbol-style :fn-native-symbol)
;                                       (render-json-ml (markup/native-reference constructor-fn)))]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  basis-template
;                                  ns-template
;                                  native-template))))
;
;(defn cljs-type-template [constructor-fn & [header-style]]
;  (let [[ns name basis] (munging/parse-constructor-info constructor-fn)
;        type-name-template (make-template :span :type-name-style name)
;        header-template (make-template :span (or header-style :type-header-style)
;                                       :type-symbol
;                                       type-name-template)
;        body-template-fn (partial cljs-type-body-template constructor-fn ns name basis)]
;    (make-template :span :type-wrapper
;                   :instance-type-header-background
;                   (make-template :span :type-ref-style
;                                  (reference-template (make-surrogate constructor-fn header-template true body-template-fn))))))
;
;(defn fetch-field [obj field]
;  [field (oget obj (munge field))])
;
;(defn fetch-instance-fields [obj basis]
;  (map (partial fetch-field obj) basis))
;
;(defn header-field-template [field]
;  (let [[name value] field]
;    (make-template :span :header-field-style
;                   (make-template :span :header-field-name-style (str name))
;                   :header-field-value-spacer
;                   (make-template :span :header-field-value-style (reference-template value))
;                   :header-field-separator)))
;
;(defn body-field-template [field]
;  (let [[name value] field]
;    (make-template :tr :body-field-tr-style
;                   (make-template :td :body-field-td1-style
;                                  :body-field-symbol
;                                  (make-template :span :body-field-name-style (str name)))
;                   (make-template :td :body-field-td2-style
;                                  :body-field-value-spacer
;                                  (make-template :span :body-field-value-style (reference-template value))))))
;
;(defn instance-fields-header-template [fields & [max-fields]]
;  (let [max-fields (or max-fields (pref :max-instance-header-fields))
;        fields-templates (map header-field-template (take max-fields fields))
;        more? (> (count fields) max-fields)]
;    (make-template :span :fields-header-style
;                   :fields-header-open-symbol
;                   fields-templates
;                   (if more? :more-fields-symbol)
;                   :fields-header-close-symbol)))
;
;(defn make-protocol-method-arity-template [arity-fn]
;  (reference-template arity-fn))
;
;(defn make-protocol-method-arities-list-body-template [fns]
;  (let [templates (map make-protocol-method-arity-template fns)
;        wrap #(make-template :li :aligned-li-style %)]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  (map wrap templates)))))
;
;(defn make-protocol-method-arities-list-template [fns & [max-fns]]
;  (let [max-fns (or max-fns (pref :max-protocol-method-arities-list))
;        header-templates (map make-protocol-method-arity-template (take max-fns fns))
;        more? (> (count fns) max-fns)
;        template (make-template :span :protocol-method-arities-header-style
;                                :protocol-method-arities-header-open-symbol
;                                (interpose :protocol-method-arities-list-header-separator header-templates)
;                                (if more? :protocol-method-arities-more-symbol)
;                                :protocol-method-arities-header-close-symbol)]
;    (if more?
;      (let [body-template-fn (partial make-protocol-method-arities-list-body-template fns)]
;        (reference-template (make-surrogate #js {} template true body-template-fn)))
;      template)))
;
;(defn make-protocol-method-template [[name fns]]
;  (make-template :span :protocol-method-style
;                 :method-icon
;                 (make-template :span :protocol-method-name-style name)
;                 (make-protocol-method-arities-list-template fns)))
;
;(defn make-protocol-body-template [obj ns _name selector _fast?]
;  (let [ns-template (if-not (empty? ns)
;                      (make-template :li :aligned-li-style
;                                     (make-template :span :protocol-ns-symbol-style :protocol-ns-symbol)
;                                     (make-template :span :protocol-ns-name-style ns)))
;        protocol-obj (munging/get-protocol-object selector)
;        native-template (if (some? protocol-obj)
;                          (make-template :li :aligned-li-style
;                                         :native-icon
;                                         (render-json-ml (markup/native-reference protocol-obj))))
;        methods (munging/collect-protocol-methods obj selector)
;        method-templates (map make-protocol-method-template methods)
;        wrap #(make-template :li :aligned-li-style %)]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  (map wrap method-templates)
;                                  ns-template
;                                  native-template))))
;
;(defn make-protocol-template [obj protocol & [style]]
;  (let [{:keys [ns name selector fast?]} protocol
;        header-template (make-template :span (or style :protocol-name-style) name)
;        main-template (make-template :span (if fast? :fast-protocol-style :slow-protocol-style)
;                                     :protocol-background)]
;    (if (some? obj)
;      (let [body-template-fn (partial make-protocol-body-template obj ns name selector fast?)]
;        (extend-template! main-template (reference-template (make-surrogate obj header-template true body-template-fn))))
;      (extend-template! main-template header-template))))
;
;(defn make-more-protocols-template [more-count]
;  (make-protocol-template nil {:name (str "+" more-count "…")} :protocol-more-style))
;
;(defn make-protocols-list-body-template [obj protocols]
;  (let [protocol-templates (map (partial make-protocol-template obj) protocols)
;        wrap #(make-template :li :aligned-li-style %)]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  (map wrap protocol-templates)))))
;
;(defn make-protocols-list-template [obj protocols & [max-protocols]]
;  (let [max-protocols (or max-protocols (pref :max-list-protocols))
;        protocols-header-templates (map (partial make-protocol-template obj) (take max-protocols protocols))
;        more-count (- (count protocols) max-protocols)
;        more? (pos? more-count)
;        template (make-template :span :protocols-header-style
;                                :protocols-list-open-symbol
;                                (interpose :header-protocol-separator protocols-header-templates)
;                                (if more? [:header-protocol-separator (make-more-protocols-template more-count)])
;                                :protocols-list-close-symbol)]
;    (if more?
;      (reference-template (make-surrogate obj template true (partial make-protocols-list-body-template obj protocols)))
;      template)))
;
;(defn instance-fields-body-template [fields obj]
;  (let [protocols (munging/scan-protocols obj)
;        has-protocols? (not (empty? protocols))
;        fields-table-template (make-template :li :aligned-li-style
;                                             :fields-icon
;                                             (make-template :table :instance-body-fields-table-style
;                                                            (map body-field-template fields)))
;        protocols-list-template (if has-protocols?
;                                  (make-template :li :aligned-li-style
;                                                 :protocols-icon
;                                                 (make-protocols-list-template obj protocols)))
;        native-template (make-template :li :aligned-li-style
;                                       :native-icon
;                                       (render-json-ml (markup/native-reference obj)))]
;    (make-template :span :body-style
;                   (make-template :ol :standard-ol-no-margin-style
;                                  fields-table-template
;                                  protocols-list-template
;                                  native-template))))
;
;(defn managed-print-via-protocol [value style]
;  (let [tmpl (make-template :span style)
;        writer (TemplateWriter. tmpl)]
;    (-pr-writer value writer {:alt-impl     alt-printer-impl
;                              :print-length (pref :max-header-elements)
;                              :more-marker  (pref :more-marker)})
;    tmpl))
;
;(defn cljs-instance-template [value]
;  (let [constructor-fn (oget value "constructor")
;        [_ns _name basis] (munging/parse-constructor-info constructor-fn)
;        custom-printing? (implements? IPrintWithWriter value)
;        type-template (cljs-type-template constructor-fn :instance-type-header-style)
;        fields (fetch-instance-fields value basis)
;        fields-header-template (instance-fields-header-template fields (if custom-printing? 0))
;        fields-body-template-fn #(instance-fields-body-template fields value)
;        instance-value-template (make-template :span :instance-value-style
;                                               (reference-template (make-surrogate value
;                                                                                   fields-header-template
;                                                                                   true
;                                                                                   fields-body-template-fn)))
;        custom-printing-template (if custom-printing?
;                                   (make-template :span :instance-custom-printing-wrapper-style
;                                                  :instance-custom-printing-background
;                                                  (managed-print-via-protocol value
;                                                                              :instance-custom-printing-style)))
;        header-template (make-template :span :instance-header-style
;                                       type-template
;                                       :instance-value-separator
;                                       instance-value-template
;                                       custom-printing-template)]
;    (reference-template (make-surrogate value header-template false))))
;
;(defn bool? [value]
;  (or (true? value) (false? value)))
;
;(defn instance-of-a-well-known-type? [value]
;  (let [well-known-types (pref :well-known-types)
;        constructor-fn (oget value "constructor")
;        [ns name] (munging/parse-constructor-info constructor-fn)
;        fully-qualified-type-name (str ns "/" name)]
;    (contains? well-known-types fully-qualified-type-name)))
;
;(defn atomic-template [value]
;  (cond
;    (nil? value) (render-json-ml (markup/<nil>))
;    (bool? value) (render-json-ml (markup/bool value))
;    (string? value) (render-json-ml (markup/string value))
;    (number? value) (render-json-ml (markup/number value))
;    (keyword? value) (render-json-ml (markup/keyword value))
;    (symbol? value) (render-json-ml (markup/symbol value))
;    (and (cljs-instance? value) (not (instance-of-a-well-known-type? value))) (cljs-instance-template value)
;    (cljs-type? value) (cljs-type-template value)
;    (cljs-function? value) (cljs-function-template value)))
;
;(defn abbreviated? [template]
;  (some #(= (pref :more-marker) %) template))
;
;(defn seq-count-is-greater-or-equal? [seq limit]
;  (let [chunk (take limit seq)]                                                                                               ; we have to be extra careful to not call count on seq, it might be an infinite sequence
;    (= (count chunk) limit)))
;
;(defn expandable? [obj]
;  (and
;    (pref :seqables-always-expandable)
;    (seqable? obj)
;    (seq-count-is-greater-or-equal? obj (pref :min-sequable-count-for-expansion))))
;
;(defn wrap-group-in-reference-if-needed [group obj]
;  (if (or (expandable? obj) (abbreviated? group))
;    (make-group (reference-template (make-surrogate obj (concat-templates! (make-template :span :header-style) group))))
;    group))
;
;(defn wrap-group-in-circular-warning-if-needed [group circular?]
;  (if circular?
;    (make-group (render-json-ml (apply markup/circular-reference (vec group))))
;    group))
;
;(defn wrap-group-in-meta-if-needed [group value]
;  (if-let [meta-data (if (pref :print-meta-data) (meta value))]
;    (make-group (render-json-ml (apply (partial markup/meta-wrapper meta-data) (vec group))))
;    group))
;
;(defn detect-edge-case-and-patch-it [group obj]
;  (cond
;    (or
;      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "\"]"))                                        ; function case
;      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "]"))                                          ; :else -constructor case
;      (and (= (count group) 3) (= (aget group 0) "#object[") (= (aget group 2) "]")))                                         ; :else -cljs$lang$ctorStr case
;    (make-group (render-json-ml (markup/native-reference obj)))
;
;    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
;    (make-group (aget group 0) (render-json-ml (markup/native-reference obj)) (aget group 2))
;
;    :else group))

;(defn wrap-value-as-reference-if-needed [value]
;  (if (or (string? value) (template? value) (group? value))
;    value
;    (reference-template value)))
;
;(defn wrap-group-values-as-references-if-needed [group]
;  (let [result (make-group)]
;    (doseq [value group]
;      (.push result (wrap-value-as-reference-if-needed value)))
;    result))

;(defn alt-printer-job [obj writer opts]
;  (if (or (safe-call satisfies? false IDevtoolsFormat obj)
;          (safe-call satisfies? false IFormat obj))                                                                           ; we have to wrap value in reference if detected IFormat
;    (-write writer (reference-template obj))
;    (if-let [tmpl (atomic-template obj)]
;      (-write writer tmpl)
;      (let [default-impl (:fallback-impl opts)
;            ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
;            inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
;        (default-impl obj writer inner-opts)))))
;
;(defn post-process-printed-output [output-group obj circular?]
;  (-> output-group
;      (detect-edge-case-and-patch-it obj)                                                                                     ; an ugly hack
;      ;      (wrap-group-values-as-references-if-needed)                                                                             ; issue #21
;      (wrap-group-in-reference-if-needed obj)
;      (wrap-group-in-circular-warning-if-needed circular?)
;      (wrap-group-in-meta-if-needed obj)))

;(defn alt-printer-impl [obj writer opts]
;  (binding [*current-state* (get-current-state)]
;    (let [circular? (is-circular? obj)
;          inner-writer (make-template-writer)]
;      (push-object-to-current-history! obj)
;      (alt-printer-job obj inner-writer opts)
;      (.merge writer (post-process-printed-output (.get-group inner-writer) obj circular?)))))

(defn build-header-markup [value]
  (markup/cljs-land (markup/header value)))

(defn build-surrogate-body-markup [value]
  (if-let [body-template (oget value "bodyTemplate")]
    body-template
    (let [target (oget value "target")]
      (if (seqable? target)
        (let [starting-index (or (oget value "startIndex") 0)]
          (markup/details target starting-index))
        (markup/standard-body-reference target)))))

; ---------------------------------------------------------------------------------------------------------------------------
; RAW API

(defn want-value?* [value]
  (cond
    (prevent-recursion?) false                                                                                                ; the value won't be rendered by our custom formatter
    :else (or (cljs-value? value) (surrogate? value))))

(defn header* [value]
  (cond
    (surrogate? value) (aget value "header")
    (safe-call satisfies? false IDevtoolsFormat value) (devtools.format/-header value)
    (safe-call satisfies? false IFormat value) (devtools.protocols/-header value)
    :else (render-markup (build-header-markup value))))

(defn has-body* [value]
  ; note: body is emulated using surrogate references
  (cond
    (surrogate? value) (aget value "hasBody")
    (safe-call satisfies? false IDevtoolsFormat value) (devtools.format/-has-body value)
    (safe-call satisfies? false IFormat value) (devtools.protocols/-has-body value)
    :else false))

(defn body* [value]
  (cond
    (surrogate? value) (render-markup (build-surrogate-body-markup value))
    (safe-call satisfies? false IDevtoolsFormat value) (devtools.format/-body value)
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
