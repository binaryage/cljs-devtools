(ns devtools.format-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils :refer [js-equals is-header]]
            [devtools.format :as f]))

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