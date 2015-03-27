(ns devtools.utils.test
  (:require [cljs.test :refer-macros [is]]
            [goog.array :as garr]
            [goog.json :as json]
            [devtools.format :as f]))

; taken from https://github.com/purnam/purnam/blob/62bec5207621779a31c5adf3593530268aebb7fd/src/purnam/native/functions.cljs#L128-L145
; Copyright © 2014 Chris Zheng
(defn js-equals [v1 v2]
  (if (= v1 v2) true
                (let [t1 (js/goog.typeOf v1)
                      t2 (js/goog.typeOf v2)]
                  (cond (= "array" t1 t2)
                        (garr/equals v1 v2 js-equals)

                        (= "object" t1 t2)
                        (let [ks1 (.sort (js-keys v1))
                              ks2 (.sort (js-keys v2))]
                          (if (garr/equals ks1 ks2)
                            (garr/every
                              ks1
                              (fn [k]
                                (js-equals (aget v1 k) (aget v2 k))))
                            false))
                        :else
                        false))))

(defn replace-refs [template placeholder]
  (let [filter (fn [key value] (if (= key "object") placeholder value))]
    (json/parse (json/serialize template filter))))

(defn collect-refs [template]
  (let [refs (atom [])
        filter (fn [key value] (if-not (= key "object")
                                 value
                                 (do
                                   (reset! refs (conj @refs value))
                                   "##REF##")))]
    (json/serialize template filter)
    @refs))

(defn want? [value expected]
  (is (= (f/want-value? value) expected) (if expected
                                           (str (pr-str value) " SHOULD be processed by devtools custom formatter")
                                           (str (pr-str value) " SHOULD NOT be processed by devtools custom formatter"))))

(defn is-template [template expected & callbacks]
  (let [sanitized-template (replace-refs template "##REF##")
        refs (collect-refs template)
        expected-template (clj->js expected)]
    (is (js-equals sanitized-template expected-template))
    (when-not (empty? callbacks)
      (is (= (count refs) (count callbacks)) "number of refs and callbacks does not match")
      (loop [rfs refs
             cbs callbacks]
        (when-not (empty? cbs)
          ((first cbs) (first rfs))
          (recur (rest rfs) (rest cbs)))))))

(defn expand-fns [expected]
  (if (coll? expected)
    (mapcat (fn [item] (cond
                         (fn? item) (item)
                         :else [item])) expected)
    expected))

(defn is-header [value expected & callbacks]
  (apply is-template (concat [(f/header-api-call value) (expand-fns expected)] callbacks)))

(defn is-body [value expected & callbacks]
  (apply is-template (concat [(f/body-api-call value) (expand-fns expected)] callbacks)))

(defn has-body? [value expected]
  (is (= (f/has-body-api-call value) expected) (if expected
                                                 (str (pr-str value) " SHOULD return true to hasBody call")
                                                 (str (pr-str value) " SHOULD return false to hasBody call"))))

(defn unroll [& args]
  (apply partial (concat [mapcat] args)))
