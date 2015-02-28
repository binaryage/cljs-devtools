(ns devtools.utils
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

(defn is-header [value expected & callbacks]
  (let [template (f/header-api-call value)
        sanitized-template (replace-refs template "##REF##")
        refs (collect-refs template)]
    (is (js-equals sanitized-template (clj->js expected)))
    (when-not (empty? callbacks)
      (is (= (count refs) (count callbacks)) "number of refs and callbacks does not match")
      (loop [rfs refs
             cbs callbacks]
        (when-not (empty? cbs)
          ((first cbs) (first rfs))
          (recur (rest rfs) (rest cbs)))))))