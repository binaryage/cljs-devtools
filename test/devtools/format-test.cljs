(ns devtools.format-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [purnam.native.functions :refer [js-equals]]
            [goog.json :as json]
            [devtools.format :as f]))

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

(deftest test-headers
  (testing "formatting headers"
    (is-header :keyword
      ["span" {"style" f/general-cljs-land-style}
       ["span" {}
        ["span" {"style" f/keyword-style} ":keyword"]]])
    (is-header 'symbol
      ["span" {"style" f/general-cljs-land-style}
       ["span" {}
        ["span" {"style" f/symbol-style} "symbol"]]])
    (is-header [1 2 3]
      ["span" {"style" f/general-cljs-land-style}
       ["span" {}
        ["span"
         {"style" "background-color:#efe"}
         "["
         ["span" {"style" f/integer-style} 1] f/spacer
         ["span" {"style" f/integer-style} 2] f/spacer
         ["span" {"style" f/integer-style} 3]
         "]"]]])
    (is-header (range 100)
      ["span" {"style" f/general-cljs-land-style}
       ["span" {}
        ["span" {"style" f/general-cljs-land-style}
         ["object" {"object" "##REF##"}]]]]
      (fn [ref]
        (is (f/surrogate? ref))
        (is-header ref
          ["span" {}
           "("
           ["span" {"style" f/integer-style} 0] f/spacer
           ["span" {"style" f/integer-style} 1] f/spacer
           ["span" {"style" f/integer-style} 2] f/spacer
           ["span" {"style" f/integer-style} 3] f/spacer
           ["span" {"style" f/integer-style} 4] f/spacer
           f/more-marker
           ")"])))
    ))