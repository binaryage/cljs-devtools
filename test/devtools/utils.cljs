(ns devtools.utils
  (:require [goog.array :as garr]))

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