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
        catch-next (atom false)
        filter (fn [_ value]
                 (when @catch-next
                   (reset! catch-next false)
                   (reset! refs (conj @refs value)))
                 (if (= value "object") (reset! catch-next true))
                 value)]
    (json/serialize template filter)
    @refs))

; note: not perfect just ad-hoc for our cases
(defn plain-js-obj? [o]
  (and (object? o) (not (coll? o))))

(defn want?* [value config expected]
  (is (= (f/want-value? value config)) expected)
  (if expected
    (str (pr-str value) " SHOULD be processed by devtools custom formatter")
    (str (pr-str value) " SHOULD NOT be processed by devtools custom formatter")))

(defn want? [value expected-or-config & rest]
  (if-not (plain-js-obj? expected-or-config)
    (apply want?* (concat [value nil expected-or-config] rest))
    (apply want?* (concat [value expected-or-config] rest))))

(defn unroll-fns [v]
  (if (vector? v)
    (mapcat (fn [item] (if (fn? item) (unroll-fns (item)) [(unroll-fns item)])) v)
    v))

(defn is-template [template expected & callbacks]
  (let [sanitized-template (replace-refs template "##REF##")
        refs (collect-refs template)
        expected-template (clj->js (unroll-fns expected))]
    (is (js-equals sanitized-template expected-template))
    (when-not (empty? callbacks)
      (is (= (count refs) (count callbacks)) "number of refs and callbacks does not match")
      (loop [rfs refs
             cbs callbacks]
        (when-not (empty? cbs)
          (let [rf (first rfs)
                object (aget rf "object")
                config (aget rf "config")
                cb (first cbs)]
            (cb object config)
            (recur (rest rfs) (rest cbs))))))))

(defn is-header* [value config expected & callbacks]
  (apply is-template (concat [(f/header value config) expected] callbacks)))

(defn is-header [value expected-or-config & rest]
  (if-not (plain-js-obj? expected-or-config)
    (apply is-header* (concat [value nil expected-or-config] rest))
    (apply is-header* (concat [value expected-or-config] rest))))

(defn is-body* [value config expected & callbacks]
  (apply is-template (concat [(f/body value config) expected] callbacks)))

(defn is-body [value expected-or-config & rest]
  (if-not (plain-js-obj? expected-or-config)
    (apply is-body* (concat [value nil expected-or-config] rest))
    (apply is-body* (concat [value expected-or-config] rest))))

(defn has-body?* [value config expected]
  (is (= (f/has-body value config) expected)
    (if expected
      (str (pr-str value) " SHOULD return true to hasBody call")
      (str (pr-str value) " SHOULD return false to hasBody call"))))

(defn has-body? [value expected-or-config & rest]
  (if-not (plain-js-obj? expected-or-config)
    (apply has-body?* (concat [value nil expected-or-config] rest))
    (apply has-body?* (concat [value expected-or-config] rest))))

(defn unroll [& args]
  (apply partial (concat [mapcat] args)))