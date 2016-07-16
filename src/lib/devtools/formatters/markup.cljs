(ns devtools.formatters.markup
  (:refer-clojure :exclude [keyword symbol meta])
  (:require [devtools.formatters.helpers :refer [pref abbreviate-long-string]]
            [devtools.formatters.helpers :refer [bool?]]
            [cljs.pprint]
            [devtools.formatters.printing :refer [managed-pr-str]]))

; reusable hiccup-like templates

(declare header)
(declare markup-map)

(defn surrogate [& args]
  (concat ["surrogate"] args))

(defn reference [& args]
  (concat ["reference"] args))

(defn reference-surrogate [& args]
  (reference (apply surrogate args)))

(defn cljs-land [& children]
  (concat [:cljs-land-tag] children))

(defn <nil> []
  [:nil-tag :nil-label])

(defn bool [bool]
  [:bool-tag bool])

(defn keyword [keyword]
  [:keyword-tag (str keyword)])

(defn symbol [symbol]
  [:symbol-tag (str symbol)])

(defn number [number]
  (if (integer? number)
    [:integer-tag number]
    [:float-tag number]))

(defn circular-reference [& children]
  (concat [:circular-reference-tag :circular-ref-icon] children))

(defn native-reference [object]
  (let [reference (reference object {:prevent-recursion true})]
    [:native-reference-tag :native-reference-background reference]))

(defn string [string]
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
        (reference-surrogate string abbreviated-string-markup true body-markup))
      [:string-tag (quote-string inline-string)])))

(defn meta [metadata]
  (let [body [:meta-body-tag (header metadata)]
        header [:meta-header-tag "meta"]]
    [:meta-reference-tag (reference-surrogate metadata header true body)]))

(defn meta-wrapper [metadata & children]
  (concat [:meta-wrapper-tag] children [(meta metadata)]))

(defn <index> [value]
  [:index-tag value :line-index-separator])

; ---------------------------------------------------------------------------------------------------------------------------

(defn header [value]
  (managed-pr-str value :header-style (pref :max-print-level) markup-map))

; ---------------------------------------------------------------------------------------------------------------------------

(defn standard-body [lines & [no-margin?]]
  (let [ol-tag (if no-margin? :standard-ol-no-margin-tag :standard-ol-tag)
        li-tag (if no-margin? :standard-li-no-margin-tag :standard-li-tag)
        * (fn [line]
            (concat [li-tag] line))]
    (concat [ol-tag] (map * lines))))

(defn body-line [index value]
  [(<index> index) (managed-pr-str value :item-style (pref :body-line-max-print-level) markup-map)])

(defn prepare-body-lines [data starting-index]
  (loop [work data
         index starting-index
         lines []]
    (if (empty? work)
      lines
      (recur (rest work) (inc index) (conj lines (body-line index (first work)))))))

(defn body-lines [value starting-index]
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
            more (reference-surrogate rest more-label true nil start-index)]
        (conj lines [more])))))

(defn details [value starting-index]
  (let [continuation? (pos? starting-index)
        body (standard-body (body-lines value starting-index) continuation?)]
    (if continuation?
      body
      [:body-tag body])))

(defn standard-body-reference [o]
  (standard-body [[(reference o)]]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn atomic [value]
  (cond
    (nil? value) (<nil>)
    (bool? value) (bool value)
    (string? value) (string value)
    (number? value) (number value)
    (keyword? value) (keyword value)
    (symbol? value) (symbol value)
    ;(and (cljs-instance? value) (not (instance-of-a-well-known-type? value))) (cljs-instance-template value)
    ;(cljs-type? value) (cljs-type-template value)
    ;(cljs-function? value) (cljs-function-template value)
    ))

(def markup-map
  {:atomic              atomic
   :reference           reference
   :surrogate           surrogate
   :reference-surrogate reference-surrogate
   :circular-reference  circular-reference
   :native-reference    native-reference
   :meta                meta
   :meta-wrapper        meta-wrapper})
