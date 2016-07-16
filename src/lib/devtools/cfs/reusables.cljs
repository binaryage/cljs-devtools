(ns devtools.cfs.reusables
  (:require [devtools.cfs.helpers :refer [pref abbreviate-long-string]]))

; reusable hiccup-like templates

(defn surrogate-markup [& args]
  (concat ["surrogate"] args))

(defn reference-markup [& args]
  (concat ["reference"] args))

(defn reference-surrogate-markup [& args]
  (reference-markup (apply surrogate-markup args)))

(defn nil-markup []
  [:nil-tag :nil-label])

(defn bool-markup [bool]
  [:bool-tag bool])

(defn keyword-markup [keyword]
  [:keyword-tag (str keyword)])

(defn symbol-markup [symbol]
  [:symbol-tag (str symbol)])

(defn number-markup [number]
  (if (integer? number)
    [:integer-tag number]
    [:float-tag number]))

(defn string-markup [string]
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
        (reference-surrogate-markup string abbreviated-string-markup true body-markup))
      [:string-tag (quote-string inline-string)])))

(defn circular-reference-markup [& children]
  (concat [:circular-reference-tag :circular-ref-icon] children))

(defn native-reference-markup [object]
  (let [reference (reference-markup object {:prevent-recursion true})]
    [:native-reference-tag :native-reference-background reference]))
