(ns devtools.formatters.markup
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]]
                   [devtools.formatters.markup :refer [emit-markup-db]])
  (:require [devtools.formatters.helpers :refer [bool? cljs-function? cljs-type? cljs-instance?
                                                 should-render-instance? expandable? abbreviated?
                                                 abbreviate-long-string get-constructor pref should-render?
                                                 get-more-marker wrap-arity fetch-fields-values]]
            [devtools.formatters.printing :refer [managed-print-via-writer managed-print-via-protocol]]
            [devtools.formatters.state :refer [set-prevent-recursion set-managed-print-level reset-depth-limits]]
            [devtools.formatters.templating :refer [get-surrogate-body
                                                    get-surrogate-target
                                                    get-surrogate-start-index
                                                    get-surrogate-header]]
            [devtools.munging :as munging]))

; reusable hiccup-like templates

(declare get-markup-db)

; -- cljs printing  ---------------------------------------------------------------------------------------------------------

(defn print-with [method value tag & [max-level]]
  (let [job-fn #(method value tag (get-markup-db))]
    (if (some? max-level)
      (binding [*print-level* (inc max-level)]                                                                                ; when printing do at most print-level deep recursion
        (job-fn))
      (job-fn))))

(defn print-via-writer [value tag & [max-level]]
  (print-with managed-print-via-writer value tag max-level))

(defn print-via-protocol [value tag & [max-level]]
  (print-with managed-print-via-protocol value tag max-level))

; -- references -------------------------------------------------------------------------------------------------------------

(defn <expandable> [& children]
  (let [inner-markup (concat [:expandable-inner-tag] children)]
    [:expandable-tag :expandable-symbol inner-markup]))

(defn <raw-surrogate> [& args]
  (concat ["surrogate"] args))

(defn <surrogate> [& [object header body start-index]]
  (let [header (if (some? body) (<expandable> header) header)]
    (<raw-surrogate> object header body start-index)))

(defn <reference> [& args]
  (concat ["reference"] args))

(defn <reference-surrogate> [& args]
  (<reference> (apply <surrogate> args)))

(defn <circular-reference> [& children]
  (<reference-surrogate> nil [:circular-reference-tag :circular-ref-icon] (concat [:circular-reference-body-tag] children)))

(defn <native-reference> [object]
  (let [reference (<reference> object #(set-prevent-recursion % true))]
    [:native-reference-wrapper-tag :native-reference-background [:native-reference-tag reference]]))

(defn <header-expander> [object]
  (<reference> (<raw-surrogate> object :header-expander-symbol :target) reset-depth-limits))

; -- simple markup ----------------------------------------------------------------------------------------------------------

(defn <cljs-land> [& children]
  (concat [:cljs-land-tag] children))

(defn <nil> []
  ; this code is duplicated in templating.cljs, see make-reference
  [:nil-tag :nil-label])

(defn <bool> [bool]
  [:bool-tag bool])

(defn <keyword> [keyword]
  [:keyword-tag (str keyword)])

(defn <symbol> [symbol]
  [:symbol-tag (str symbol)])

(defn <number> [number]
  (if (integer? number)
    [:integer-tag number]
    [:float-tag number]))

; -- string markup ----------------------------------------------------------------------------------------------------------

(defn <string> [string]
  (let [dq (pref :dq)
        re-nl (js/RegExp. "\n" "g")
        nl-marker (pref :new-line-string-replacer)
        inline-string (.replace string re-nl nl-marker)
        max-inline-string-size (+ (pref :string-prefix-limit) (pref :string-postfix-limit))
        quote-string (fn [s] (str dq s dq))
        should-abbreviate? (> (count inline-string) max-inline-string-size)]
    (if should-abbreviate?
      (let [abbreviated-string (abbreviate-long-string inline-string
                                                       (pref :string-abbreviation-marker)
                                                       (pref :string-prefix-limit)
                                                       (pref :string-postfix-limit))
            abbreviated-string-markup [:string-tag (quote-string abbreviated-string)]
            string-with-nl-markers (.replace string re-nl (str nl-marker "\n"))
            details-markup [:expanded-string-tag string-with-nl-markers]]
        (<reference-surrogate> string abbreviated-string-markup details-markup))
      [:string-tag (quote-string inline-string)])))

; -- generic preview markup -------------------------------------------------------------------------------------------------

(defn <preview> [value]
  (print-via-writer value :header-tag (pref :max-print-level)))

; -- body-related templates -------------------------------------------------------------------------------------------------

(defn <body> [markup]
  [:body-tag markup])

(defn <aligned-body> [markups-lists]
  (let [prepend-li-tag (fn [line]
                         (if line
                           (concat [:aligned-li-tag] line)))
        aligned-lines (keep prepend-li-tag markups-lists)]
    (<body> (concat [:standard-ol-no-margin-tag] aligned-lines))))

(defn <standard-body> [markups-lists & [no-margin?]]
  (let [ol-tag (if no-margin? :standard-ol-no-margin-tag :standard-ol-tag)
        li-tag (if no-margin? :standard-li-no-margin-tag :standard-li-tag)
        prepend-li-tag (fn [line]
                         (if line
                           (concat [li-tag] line)))
        lines-markups (keep prepend-li-tag markups-lists)]
    (concat [ol-tag] lines-markups)))

(defn <standard-body-reference> [o]
  (<standard-body> [[(<reference> o)]]))

; -- generic details markup -------------------------------------------------------------------------------------------------

(defn <index> [value]
  [:index-tag value :line-index-separator])

(defn- body-line [index value]
  (let [index-markup (<index> index)
        value-markup (print-via-writer value :item-tag (pref :body-line-max-print-level))]
    [index-markup value-markup]))

; TODO: this fn is screaming for rewrite
(defn- prepare-body-lines [data starting-index]
  (loop [work data
         index starting-index
         lines []]
    (if (empty? work)
      lines
      (recur (rest work) (inc index) (conj lines (body-line index (first work)))))))

(defn- body-lines [value starting-index]
  (let [seq (seq value)
        max-number-body-items (pref :max-number-body-items)
        chunk (take max-number-body-items seq)
        rest (drop max-number-body-items seq)
        lines (prepare-body-lines chunk starting-index)
        continue? (not (empty? (take 1 rest)))]
    (if-not continue?
      lines
      (let [more-label-markup [:body-items-more-tag :body-items-more-label]
            start-index (+ starting-index max-number-body-items)
            more-markup (<reference-surrogate> rest more-label-markup :target start-index)]
        (conj lines [more-markup])))))

(defn <details> [value starting-index]
  (let [has-continuation? (pos? starting-index)
        body-markup (<standard-body> (body-lines value starting-index) has-continuation?)]
    (if has-continuation?
      body-markup
      (<body> body-markup))))

; -- generic list template --------------------------------------------------------------------------------------------------

(defn <list-details> [items _opts]
  (<aligned-body> (map list items)))

(defn <list> [items max-count & [opts]]
  (let [items-markups (take max-count items)
        more-count (- (count items) max-count)
        more? (pos? more-count)
        separator (or (:separator opts) :list-separator)
        more-symbol (if more?
                      (if-let [more-symbol (:more-symbol opts)]
                        (if (fn? more-symbol)
                          (more-symbol more-count)
                          more-symbol)
                        (get-more-marker more-count)))
        preview-markup (concat [(or (:tag opts) :list-tag)
                                (or (:open-symbol opts) :list-open-symbol)]
                               (interpose separator items-markups)
                               (if more? [separator more-symbol])
                               [(or (:close-symbol opts) :list-close-symbol)])]
    (if more?
      (let [details-markup (:details opts)
            default-details-fn (partial <list-details> items opts)]
        (<reference-surrogate> nil preview-markup (or details-markup default-details-fn)))
      preview-markup)))

; -- mete-related markup ----------------------------------------------------------------------------------------------------

(defn <meta> [metadata]
  (let [body [:meta-body-tag (<preview> metadata)]
        header [:meta-header-tag "meta"]]
    [:meta-reference-tag (<reference-surrogate> metadata header body)]))

(defn <meta-wrapper> [metadata & children]
  (concat [:meta-wrapper-tag] children [(<meta> metadata)]))

; -- function markup --------------------------------------------------------------------------------------------------------

(defn <function-details> [fn-obj ns _name arities prefix]
  {:pre [(fn? fn-obj)]}
  (let [arities (map wrap-arity arities)
        make-arity-markup-list (fn [arity]
                                 [[:fn-multi-arity-args-indent-tag prefix]
                                  [:fn-args-tag arity]])
        arities-markupts-lists (if (> (count arities) 1) (map make-arity-markup-list arities))
        ns-markups-list (if-not (empty? ns) [:ns-icon [:fn-ns-name-tag ns]])
        native-markups-list [:native-icon (<native-reference> fn-obj)]]
    (<aligned-body> (concat arities-markupts-lists [ns-markups-list native-markups-list]))))

(defn <arities> [arities]
  (let [multi-arity? (> (count arities) 1)]
    [:fn-args-tag (wrap-arity (if multi-arity?
                                (pref :multi-arity-symbol)
                                (first arities)))]))

(defn <function> [fn-obj]
  {:pre [(fn? fn-obj)]}
  (let [[ns name] (munging/parse-fn-info fn-obj)
        lambda? (empty? name)
        spacer-symbol (pref :spacer)
        rest-symbol (pref :rest-symbol)
        multi-arity-symbol (pref :multi-arity-symbol)
        arities (munging/extract-arities fn-obj true spacer-symbol multi-arity-symbol rest-symbol)
        arities-markup (<arities> arities)
        name-markup (if-not lambda? [:fn-name-tag name])
        icon-markup (if lambda? :lambda-icon :fn-icon)
        prefix-markup [:fn-prefix-tag icon-markup name-markup]
        preview-markup [:fn-header-tag prefix-markup arities-markup]
        details-fn (partial <function-details> fn-obj ns name arities prefix-markup)]
    (<reference-surrogate> fn-obj preview-markup details-fn)))

; -- type markup ------------------------------------------------------------------------------------------------------------

(defn <type-basis-item> [basis-item]
  [:type-basis-item-tag (name basis-item)])

(defn <type-basis> [basis]
  (let [item-markups (map <type-basis-item> basis)
        children-markups (interpose :type-basis-item-separator item-markups)]
    (concat [:type-basis-tag] children-markups)))

(defn <type-details> [constructor-fn ns _name basis]
  (let [ns-markup (if-not (empty? ns) [:ns-icon [:fn-ns-name-tag ns]])
        basis-markup (if (empty? basis)
                       [:empty-basis-symbol]
                       [:basis-icon (<type-basis> basis)])
        native-markup [:native-icon (<native-reference> constructor-fn)]]
    (<aligned-body> [basis-markup ns-markup native-markup])))

(defn <type> [constructor-fn & [header-tag]]
  (let [[ns name basis] (munging/parse-constructor-info constructor-fn)
        name-markup [:type-name-tag name]
        preview-markup [(or header-tag :type-header-tag) :type-symbol name-markup]
        details-markup-fn (partial <type-details> constructor-fn ns name basis)]
    [:type-wrapper-tag
     :type-header-background
     [:type-ref-tag (<reference-surrogate> constructor-fn preview-markup details-markup-fn)]]))

(defn <standalone-type> [constructor-fn & [header-tag]]
  [:standalone-type-tag (<type> constructor-fn header-tag)])

; -- protocols markup -------------------------------------------------------------------------------------------------------

(defn <protocol-method-arity> [arity-fn]
  (<reference> arity-fn))

(defn <protocol-method-arities-details> [fns]
  (<aligned-body> (map <protocol-method-arity> fns)))

(defn <protocol-method-arities> [fns & [max-fns]]
  (let [max-fns (or max-fns (pref :max-protocol-method-arities-list))
        more? (> (count fns) max-fns)
        aritites-markups (map <protocol-method-arity> (take max-fns fns))
        preview-markup (concat [:protocol-method-arities-header-tag :protocol-method-arities-header-open-symbol]
                               (interpose :protocol-method-arities-list-header-separator aritites-markups)
                               (if more? [:protocol-method-arities-more-symbol])
                               [:protocol-method-arities-header-close-symbol])]
    (if more?
      (let [details-markup-fn (partial <protocol-method-arities-details> fns)]
        (<reference-surrogate> nil preview-markup details-markup-fn))
      preview-markup)))

(defn <protocol-method> [name arities]
  [:protocol-method-tag
   :method-icon
   [:protocol-method-name-tag name]
   (<protocol-method-arities> arities)])

(defn <protocol-details> [obj ns _name selector _fast?]
  (let [protocol-obj (munging/get-protocol-object selector)
        ns-markups-list (if-not (empty? ns) [:ns-icon [:protocol-ns-name-tag ns]])
        native-markups-list (if (some? protocol-obj) [:native-icon (<native-reference> protocol-obj)])
        methods (munging/collect-protocol-methods obj selector)
        methods-markups (map (fn [[name arities]] (<protocol-method> name arities)) methods)
        methods-markups-lists (map list methods-markups)]
    (<aligned-body> (concat methods-markups-lists [ns-markups-list native-markups-list]))))

(defn <protocol> [obj protocol & [tag]]
  (let [{:keys [ns name selector fast?]} protocol
        preview-markup [(or tag :protocol-name-tag) name]
        prefix-markup [(if fast? :fast-protocol-tag :slow-protocol-tag) :protocol-background]]
    (if (some? obj)
      (let [details-markup-fn (partial <protocol-details> obj ns name selector fast?)]
        (conj prefix-markup (<reference-surrogate> obj preview-markup details-markup-fn)))
      (conj prefix-markup preview-markup))))

(defn <more-protocols> [more-count]
  (let [fake-protocol {:name (get-more-marker more-count)}]
    (<protocol> nil fake-protocol :protocol-more-tag)))

(defn <protocols-list> [obj protocols & [max-protocols]]
  (let [max-protocols (or max-protocols (pref :max-list-protocols))
        protocols-markups (map (partial <protocol> obj) protocols)]
    (<list> protocols-markups max-protocols {:tag          :protocols-header-tag
                                             :open-symbol  :protocols-list-open-symbol
                                             :close-symbol :protocols-list-close-symbol
                                             :separator    :header-protocol-separator
                                             :more-symbol  <more-protocols>})))

; -- instance fields markup -------------------------------------------------------------------------------------------------

(defn <field> [name value]
  [:header-field-tag
   [:header-field-name-tag (str name)]
   :header-field-value-spacer
   [:header-field-value-tag (<reference> (<surrogate> value) #(set-managed-print-level % 1))]
   :header-field-separator])

(defn <fields-details-row> [field]
  (let [[name value] field]
    [:body-field-tr-tag
     [:body-field-td1-tag
      :body-field-symbol
      [:body-field-name-tag (str name)]]
     [:body-field-td2-tag
      :body-field-value-spacer]
     [:body-field-td3-tag
      [:body-field-value-tag (<reference-surrogate> value)]]]))

(defn <fields> [fields & [max-fields]]
  (if (zero? (count fields))
    [:fields-header-tag :fields-header-no-fields-symbol]
    (let [max-fields (or max-fields (pref :max-instance-header-fields))
          more? (> (count fields) max-fields)
          fields-markups (map (fn [[name value]] (<field> name value)) (take max-fields fields))]
      (concat [:fields-header-tag
               :fields-header-open-symbol]
              fields-markups
              [(if more? :more-fields-symbol)
               :fields-header-close-symbol]))))

(defn <fields-details> [fields obj]
  (let [protocols (munging/scan-protocols obj)
        has-protocols? (not (empty? protocols))
        fields-markup (if-not (zero? (count fields))
                        [:fields-icon (concat [:instance-body-fields-table-tag] (map <fields-details-row> fields))])
        protocols-list-markup (if has-protocols? [:protocols-icon (<protocols-list> obj protocols)])
        native-markup [:native-icon (<native-reference> obj)]]
    (<aligned-body> [fields-markup protocols-list-markup native-markup])))

; -- type/record instance markup --------------------------------------------------------------------------------------------

(defn <instance> [value]
  (let [constructor-fn (get-constructor value)
        [_ns _name basis] (munging/parse-constructor-info constructor-fn)
        custom-printing? (implements? IPrintWithWriter value)
        type-markup (<type> constructor-fn :instance-type-header-tag)
        fields (fetch-fields-values value basis)
        fields-markup (<fields> fields (if custom-printing? 0))
        fields-details-markup-fn #(<fields-details> fields value)
        fields-preview-markup [:instance-value-tag (<reference-surrogate> value fields-markup fields-details-markup-fn)]
        custom-printing-markup (if custom-printing?
                                 [:instance-custom-printing-wrapper-tag
                                  :instance-custom-printing-background
                                  (print-via-protocol value :instance-custom-printing-tag)])]
    [:instance-header-tag
     :instance-header-background
     fields-preview-markup
     custom-printing-markup
     type-markup]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn <header> [value]
  (<cljs-land> (<preview> value)))

(defn <surrogate-header> [surrogate]
  (or (get-surrogate-header surrogate)
      (<preview> (get-surrogate-target surrogate))))

(defn <surrogate-target> [surrogate]
  (let [target (get-surrogate-target surrogate)]
    (if (seqable? target)
      (let [starting-index (get-surrogate-start-index surrogate)]
        (<details> target starting-index))
      (<standard-body-reference> target))))

(defn <surrogate-body> [surrogate]
  (if-let [body (get-surrogate-body surrogate)]
    (if (= :target body)
      (<surrogate-target> surrogate)
      body)))

; ---------------------------------------------------------------------------------------------------------------------------

(defn <atomic> [value]
  (cond
    (should-render? :render-nils value nil?) (<nil>)
    (should-render? :render-bools value bool?) (<bool> value)
    (should-render? :render-strings value string?) (<string> value)
    (should-render? :render-numbers value number?) (<number> value)
    (should-render? :render-keywords value keyword?) (<keyword> value)
    (should-render? :render-symbols value symbol?) (<symbol> value)
    (should-render? :render-instances value should-render-instance?) (<instance> value)
    (should-render? :render-types value cljs-type?) (<standalone-type> value)
    (should-render? :render-functions value cljs-function?) (<function> value)))

; ---------------------------------------------------------------------------------------------------------------------------

(def ^:dynamic *markup-db*)

; emit-markup-db macro will generate a map of all markup <functions> in this namespace:
;
;    {:atomic              <atomic>
;     :reference           <reference>
;     :native-reference    <native-reference>
;     ...}
;
; we generate it only on first call and cache it in *markup-db*
; emitting markup db statically into def would prevent dead-code elimination
;
(defn get-markup-db []
  (if (nil? *markup-db*)
    (set! *markup-db* (emit-markup-db)))
  *markup-db*)
