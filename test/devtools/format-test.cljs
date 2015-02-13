(ns devtools.format-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [purnam.native.functions :refer [js-equals]]
            [devtools.format :as format]))

(defn is-header [value expected]
  (is (js-equals (format/header-api-call value) (clj->js expected))))

(deftest test-headers
  (testing "formatting headers"
    (is-header :keyword
      ["span" {"style" "background-color:#efe"}
       ["span" {}
        ["span" {"style" "color:#881391"} ":keyword"]]])
    (is-header 'symbol
      ["span" {"style" "background-color:#efe"}
       ["span" {}
        ["span" {"style" "color:#000000"} "symbol"]]])
    (is-header [1 2 3]
      ["span" {"style" "background-color:#efe"}
       ["span" {}
        ["span"
         {"style" "background-color:#efe"}
         "["
         ["span" {"style" "color:#1C00CF"} 1]
         " "
         ["span" {"style" "color:#1C00CF"} 2]
         " "
         ["span" {"style" "color:#1C00CF"} 3]
         "]"]]])
    ))
