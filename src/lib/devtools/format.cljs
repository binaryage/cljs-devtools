(ns devtools.format
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.munging :as munging]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]))

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

(defn cljs-function? [value]
  (and (not (pref :disable-cljs-fn-formatting))
       (not (var? value))                                                                                                     ; HACK: vars have IFn protocol and would act as functions TODO: implement custom rendering for vars
       (munging/cljs-fn? value)))

; IRC #clojurescript @ freenode.net on 2015-01-27:
; [13:40:09] darwin_: Hi, what is the best way to test if I'm handled ClojureScript data value or plain javascript object?
; [14:04:34] dnolen: there is a very low level thing you can check
; [14:04:36] dnolen: https://github.com/clojure/clojurescript/blob/c2550c4fdc94178a7957497e2bfde54e5600c457/src/clj/cljs/core.clj#L901
; [14:05:00] dnolen: this property is unlikely to change - still it's probably not something anything anyone should use w/o a really good reason
(defn cljs-value? [value]
  (or
    (if (goog/isObject value)                                                                                                 ; see http://stackoverflow.com/a/22482737/84283
      (oget value "constructor" "cljs$lang$type"))
    (cljs-function? value)))

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
          (.push template (resolve-pref child)))))
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

(defn ^bool is-circular?! [object]
  (let [current-state (get-current-state)]
    (if (:entered-reference current-state)
      (do
        ; we skip the cicularity check after entering into a reference
        ; reference's expandable placeholder was already checked and marked if circular
        (update-current-state! dissoc :entered-reference)
        false)
      (let [history (get-current-history)]
        (some #(identical? % object) history)))))

(defn circular-reference-template [content-group]
  (let [base-template (make-template :span :circular-reference-wrapper-style
                                     (make-template :span :circular-reference-symbol-style :circular-reference-symbol))]
    (concat-templates! base-template content-group)))

(defn reference [object & [state-override]]
  (make-group "object" #js {"object" object
                            "config" (merge (get-current-state) {:entered-reference true} state-override)}))

(defn reference? [value]
  (and (group? value)
       (= (aget value 0) "object")))

(defn native-reference [object]
  (reference object {:prevent-recursion true}))

(defn index-template [value]
  (make-template :span :index-style value :line-index-separator))

(defn number-template [value]
  (if (integer? value)
    (make-template :span :integer-style value)
    (make-template :span :float-style value)))

(declare build-header)

(defn meta-template [value]
  (let [header-template (make-template :span :meta-style "meta")
        body-template (make-template :span :meta-body-style
                                     (build-header value))]
    (make-template :span :meta-reference-style (reference (make-surrogate value header-template true body-template)))))

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
        (reference (make-surrogate source-string abbreviated-string-template true body-template))))))

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
                                       (native-reference fn-obj))]
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
    (reference (make-surrogate fn-obj header-template true body-template))))

(defn bool? [value]
  (or (true? value) (false? value)))

(defn atomic-template [value]
  (cond
    (nil? value) (make-template :span :nil-style :nil-label)
    (bool? value) (make-template :span :bool-style value)
    (string? value) (string-template value)
    (number? value) (number-template value)
    (keyword? value) (make-template :span :keyword-style (str value))
    (symbol? value) (make-template :span :symbol-style (str value))
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

(deftype TemplateWriter [group]
  Object
  (merge [_ a] (.apply (.-push group) group a))
  (get-group [_] group)
  IWriter
  (-write [_ o] (.push group o))
  (-flush [_] nil))

(defn make-template-writer []
  (TemplateWriter. (make-group)))

(defn wrap-group-in-reference-if-needed [group obj]
  (if (or (expandable? obj) (abbreviated? group))
    (make-group (reference (make-surrogate obj (concat-templates! (make-template :span :header-style) group))))
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
    (make-group (native-reference obj))

    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
    (make-group (aget group 0) (native-reference obj) (aget group 2))

    :else group))

(defn wrap-value-as-reference-if-needed [value]
  (if (or (string? value) (template? value) (group? value))
    value
    (reference value)))

(defn wrap-group-values-as-references-if-needed [group]
  (let [result (make-group)]
    (doseq [value group]
      (.push result (wrap-value-as-reference-if-needed value)))
    result))

(defn alt-printer-impl [obj writer opts]
  (binding [*current-state* (get-current-state)]
    (if (and (not (:entered-reference *current-state*))
             (or (safe-call satisfies? false IDevtoolsFormat obj)
                 (safe-call satisfies? false IFormat obj)))                                                                   ; we have to wrap value in reference if detected IFormat
      (-write writer (reference obj))                                                                                         ; :entered-reference is here for prevention of infinite recursion
      (let [circular? (is-circular?! obj)]
        (push-object-to-current-history! obj)
        (if-let [tmpl (atomic-template obj)]
          (-write writer tmpl)
          (let [inner-writer (make-template-writer)
                default-impl (:fallback-impl opts)
                ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
                inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
            (default-impl obj inner-writer inner-opts)
            (let [final-group (-> (.get-group inner-writer)
                                  (detect-edge-case-and-patch-it obj)                                                         ; an ugly hack
                                  (wrap-group-values-as-references-if-needed)                                                 ; issue #21
                                  (wrap-group-in-reference-if-needed obj)
                                  (wrap-group-in-circular-warning-if-needed circular?))]
              (.merge writer final-group))))))))

(defn managed-pr-str [value style print-level]
  (let [tmpl (make-template :span style)
        writer (TemplateWriter. tmpl)]
    (binding [*print-level* print-level]                                                                                      ; when printing do at most print-level deep recursion
      (pr-seq-writer [value] writer {:alt-impl     alt-printer-impl
                                     :print-length (pref :max-header-elements)
                                     :more-marker  (pref :more-marker)}))
    tmpl))

(defn build-header [value]
  (let [value-template (managed-pr-str value :header-style (inc (pref :max-print-level)))]
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
  [(index-template index) (managed-pr-str value :item-style (inc (pref :body-line-max-print-level)))])

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
        (conj lines (reference surrogate-object))))))

(defn build-body [value starting-index]
  (let [is-body? (zero? starting-index)
        result (standard-body-template (body-lines-templates value starting-index) is-body?)]
    (if is-body?
      (make-template :span :body-style result)
      result)))

(defn standard-reference [target]
  (make-template :ol :standard-ol-style (make-template :li :standard-li-style (reference target))))

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
