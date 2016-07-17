(ns devtools.formatters.markup
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]]
                   [devtools.formatters.markup :refer [emit-markup-map]])
  (:require [cljs.pprint]
            [devtools.formatters.helpers :refer [bool? cljs-function? pref abbreviate-long-string cljs-type? cljs-instance?
                                                 instance-of-a-well-known-type?]]
            [devtools.formatters.printing :refer [managed-pr-str managed-print-via-protocol]]
            [devtools.munging :as munging]))

; reusable hiccup-like templates

(declare markup-map)

(defn <surrogate> [& args]
  (concat ["surrogate"] args))

(defn <reference> [& args]
  (concat ["reference"] args))

(defn <reference-surrogate> [& args]
  (<reference> (apply <surrogate> args)))

(defn <preview> [value]
  (managed-pr-str value :header-style (pref :max-print-level) markup-map))

(defn <cljs-land> [& children]
  (concat [:cljs-land-tag] children))

(defn <nil> []
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

(defn <circular-reference> [& children]
  (concat [:circular-reference-tag :circular-ref-icon] children))

(defn <native-reference> [object]
  (let [reference (<reference> object {:prevent-recursion true})]
    [:native-reference-tag :native-reference-background reference]))

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
            body-markup [:expanded-string-tag string-with-nl-markers]]
        (<reference-surrogate> string abbreviated-string-markup true body-markup))
      [:string-tag (quote-string inline-string)])))

(defn <meta> [metadata]
  (let [body [:meta-body-tag (<preview> metadata)]
        header [:meta-header-tag "meta"]]
    [:meta-reference-tag (<reference-surrogate> metadata header true body)]))

(defn <meta-wrapper> [metadata & children]
  (concat [:meta-wrapper-tag] children [(<meta> metadata)]))

(defn <index> [value]
  [:index-tag value :line-index-separator])

(defn <aligned-body> [lines]
  (let [align (fn [line]
                (if line
                  (concat [:aligned-li-tag] line)))
        aligned-lines (keep align lines)]
    [:body-tag
     (concat [:standard-ol-no-margin-tag] aligned-lines)]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn- wrap-arity [arity]
  (let [args-open-symbol (pref :args-open-symbol)
        args-close-symbol (pref :args-close-symbol)]
    (str args-open-symbol arity args-close-symbol)))

(defn <arity> [prefix arity]
  [[:fn-multi-arity-args-indent-tag prefix]
   [:fn-args-tag arity]])

(defn <function-details> [fn-obj ns _name arities prefix]
  {:pre [(fn? fn-obj)]}
  (let [arities (map wrap-arity arities)
        arities (if (> (count arities) 1)
                  (map (partial <arity> prefix) arities))
        ns (if-not (empty? ns)
             [:ns-icon
              [:fn-ns-name-tag ns]])
        native [:native-icon (<native-reference> fn-obj)]]
    (<aligned-body> (concat arities [ns native]))))

(defn <arities> [arities]
  (let [multi-arity? (> (count arities) 1)]
    [:fn-args-tag (wrap-arity (if multi-arity?
                                (pref :multi-arity-symbol)
                                (first arities)))]))

(defn <function> [fn-obj]
  {:pre [(fn? fn-obj)]}
  (let [[ns name] (munging/parse-fn-info fn-obj)
        spacer-symbol (pref :spacer)
        rest-symbol (pref :rest-symbol)
        multi-arity-symbol (pref :multi-arity-symbol)
        arities (munging/extract-arities fn-obj true spacer-symbol multi-arity-symbol rest-symbol)
        arities-markup (<arities> arities)
        lambda? (empty? name)
        name-markup (if-not lambda? [:fn-name-tag name])
        icon (if lambda? :lambda-icon :fn-icon)
        prefix [:fn-prefix-tag icon name-markup]
        preview [:fn-header-tag prefix arities-markup]
        details (partial <function-details> fn-obj ns name arities prefix)]
    (<reference-surrogate> fn-obj preview true details)))

; ---------------------------------------------------------------------------------------------------------------------------

(defn <type-basis-item> [basis-item]
  [:type-basis-item-tag (name basis-item)])

(defn <type-basis> [basis]
  (let [item-markups (map <type-basis-item> basis)
        children-markups (interpose :type-basis-item-separator item-markups)]
    (concat [:type-basis-tag] children-markups)))

(defn <type-details> [constructor-fn ns _name basis]
  (let [ns-markup (if-not (empty? ns) [:ns-icon [:fn-ns-name-tag ns]])
        basis-markup (if-not (empty? basis) [:basis-icon (<type-basis> basis)])
        native-markup [:native-icon (<native-reference> constructor-fn)]]
    (<aligned-body> [basis-markup ns-markup native-markup])))

(defn <type> [constructor-fn & [header-style]]
  (let [[ns name basis] (munging/parse-constructor-info constructor-fn)
        name-markup [:type-name-tag name]
        preview-markup [[:span (or header-style :type-header-style)] :type-symbol name-markup]
        details-markup-fn (partial <type-details> constructor-fn ns name basis)]
    [:type-wrapper-tag
     :type-header-background
     [:type-ref-tag (<reference-surrogate> constructor-fn preview-markup true details-markup-fn)]]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn- fetch-field-value [obj field]
  [field (oget obj (munge field))])

(defn- fetch-fields-values [obj fields]
  (map (partial fetch-field-value obj) fields))

(defn <field> [field]
  (let [[name value] field]
    [:header-field-tag
     [:header-field-name-tag (str name)]
     :header-field-value-spacer
     [:header-field-value-tag (<reference> value)]
     :header-field-separator]))

(defn <fields-details-row> [field]
  (let [[name value] field]
    [:body-field-tr-tag
     [:body-field-td1-tag
      :body-field-symbol
      [:body-field-name-tag (str name)]]
     [:body-field-td2-tag
      :body-field-value-spacer
      [:body-field-value-tag (<reference> value)]]]))

(defn <fields> [fields & [max-fields]]
  (let [max-fields (or max-fields (pref :max-instance-header-fields))
        fields-markups (map <field> (take max-fields fields))
        more? (> (count fields) max-fields)]
    (concat [:fields-header-tag
             :fields-header-open-symbol]
            fields-markups
            [(if more? :more-fields-symbol)
             :fields-header-close-symbol])))

(defn <protocol-method-arity> [arity-fn]
  (<reference> arity-fn))

(defn <protocol-method-arities-details> [fns]
  (<aligned-body> (map <protocol-method-arity> fns)))

(defn <protocol-method-arities> [fns & [max-fns]]
  (let [max-fns (or max-fns (pref :max-protocol-method-arities-list))
        aritites-markups (map <protocol-method-arity> (take max-fns fns))
        more? (> (count fns) max-fns)
        preview-markup (concat [:protocol-method-arities-header-tag :protocol-method-arities-header-open-symbol]
                               (interpose :protocol-method-arities-list-header-separator aritites-markups)
                               (if more? [:protocol-method-arities-more-symbol])
                               [:protocol-method-arities-header-close-symbol])]
    (if more?
      (let [details-markup-fn (partial <protocol-method-arities-details> fns)]
        (<reference-surrogate> #js {} preview-markup true details-markup-fn))
      preview-markup)))

(defn <protocol-method> [[name fns]]
  [:protocol-method-tag
   :method-icon
   [:protocol-method-name-tag name]
   (<protocol-method-arities> fns)])

(defn <protocol-details> [obj ns _name selector _fast?]
  (let [ns-markup (if-not (empty? ns) [:ns-icon [:protocol-ns-name-tag ns]])
        protocol-obj (munging/get-protocol-object selector)
        native-markup (if (some? protocol-obj) [:native-icon (<native-reference> protocol-obj)])
        methods (munging/collect-protocol-methods obj selector)
        methods-markups (map <protocol-method> methods)
        wrap (fn [x] [x])]
    (<aligned-body> (concat (map wrap methods-markups) [ns-markup native-markup]))))

(defn <protocol> [obj protocol & [style]]
  (let [{:keys [ns name selector fast?]} protocol
        preview-markup [[:span (or style :protocol-name-style)] name]
        prefix-markup [[:span (if fast? :fast-protocol-style :slow-protocol-style)] :protocol-background]]
    (if (some? obj)
      (let [details-markup-fn (partial <protocol-details> obj ns name selector fast?)]
        (conj prefix-markup (<reference-surrogate> obj preview-markup true details-markup-fn)))
      (conj prefix-markup preview-markup))))

(defn <more-protocols> [more-count]
  (let [fake-protocol {:name (str "+" more-count "…")}]
    (<protocol> nil fake-protocol :protocol-more-style)))

(defn <protocols-list-details> [obj protocols]
  (let [protocols-markups (map (partial <protocol> obj) protocols)
        wrap (fn [x] [x])]
    (<aligned-body> (map wrap protocols-markups))))

(defn <protocols-list> [obj protocols & [max-protocols]]
  (let [max-protocols (or max-protocols (pref :max-list-protocols))
        protocols-markups (map (partial <protocol> obj) (take max-protocols protocols))
        more-count (- (count protocols) max-protocols)
        more? (pos? more-count)
        preview-markup (concat [:protocols-header-tag :protocols-list-open-symbol]
                               (interpose :header-protocol-separator protocols-markups)
                               (if more? [:header-protocol-separator (<more-protocols> more-count)])
                               [:protocols-list-close-symbol])]
    (if more?
      (let [details-markup-fn (partial <protocols-list-details> obj protocols)]
        (<reference-surrogate> obj preview-markup true details-markup-fn))
      preview-markup)))

(defn <fields-details> [fields obj]
  (let [protocols (munging/scan-protocols obj)
        has-protocols? (not (empty? protocols))
        fields-markup [:fields-icon (concat [:instance-body-fields-table-tag] (map <fields-details-row> fields))]
        protocols-list-markup (if has-protocols? [:protocols-icon (<protocols-list> obj protocols)])
        native-markup [:native-icon (<native-reference> obj)]]
    (<aligned-body> [fields-markup protocols-list-markup native-markup])))

(defn <instance> [value]
  (let [constructor-fn (oget value "constructor")
        [_ns _name basis] (munging/parse-constructor-info constructor-fn)
        custom-printing? (implements? IPrintWithWriter value)
        type-template (<type> constructor-fn :instance-type-header-style)
        fields (fetch-fields-values value basis)
        fields-markup (<fields> fields (if custom-printing? 0))                                                               ; TODO: handle no fields properly
        fields-details-markup-fn #(<fields-details> fields value)
        fields-preview-markup [:instance-value-tag (<reference-surrogate> value fields-markup true fields-details-markup-fn)]
        custom-printing-markup (if custom-printing?
                                 [:instance-custom-printing-wrapper-tag
                                  :instance-custom-printing-background
                                  (managed-print-via-protocol value :instance-custom-printing-style markup-map)])
        preview-markup [:instance-header-tag
                        type-template
                        :instance-value-separator
                        fields-preview-markup
                        custom-printing-markup]]
    (<reference-surrogate> value preview-markup false)))

; ---------------------------------------------------------------------------------------------------------------------------

(defn <standard-body> [lines & [no-margin?]]
  (let [ol-tag (if no-margin? :standard-ol-no-margin-tag :standard-ol-tag)
        li-tag (if no-margin? :standard-li-no-margin-tag :standard-li-tag)]
    (concat [ol-tag] (map #(concat [li-tag] %) lines))))

(defn- body-line [index value]
  [(<index> index) (managed-pr-str value :item-style (pref :body-line-max-print-level) markup-map)])

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
      (let [more-label [:body-items-more-tag (pref :body-items-more-label)]
            start-index (+ starting-index max-number-body-items)
            more (<reference-surrogate> rest more-label true nil start-index)]
        (conj lines [more])))))

(defn <details> [value starting-index]
  (let [continuation? (pos? starting-index)
        body (<standard-body> (body-lines value starting-index) continuation?)]
    (if continuation?
      body
      [:body-tag body])))

(defn <standard-body-reference> [o]
  (<standard-body> [[(<reference> o)]]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn <atomic> [value]
  (cond
    (nil? value) (<nil>)
    (bool? value) (<bool> value)
    (string? value) (<string> value)
    (number? value) (<number> value)
    (keyword? value) (<keyword> value)
    (symbol? value) (<symbol> value)
    (and (cljs-instance? value) (not (instance-of-a-well-known-type? value))) (<instance> value)
    (cljs-type? value) (<type> value)
    (cljs-function? value) (<function> value)))

; ---------------------------------------------------------------------------------------------------------------------------

(def markup-map
  {:atomic              <atomic>
   :reference           <reference>
   :surrogate           <surrogate>
   :reference-surrogate <reference-surrogate>
   :circular-reference  <circular-reference>
   :native-reference    <native-reference>
   :meta                <meta>
   :meta-wrapper        <meta-wrapper>})
