(ns devtools.format-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.test-utils :refer [js-equals is-header want?]]
            [devtools.format :as f]))

(deftest wants
  (testing "these simple values should not be processed by our custom formatter"
    (want? "some string" false)
    (want? 0 false)
    (want? 1000 false)
    (want? -1000 false)
    (want? 0.5 false)
    (want? 0.0 false)
    (want? -0.5 false)
    (want? true false)
    (want? false false)
    (want? nil false)
    (want? #(.-document js/window) false))
  (testing "these values should be processed by our custom formatter"
    (want? :keyword true)
    (want? 'symbol true)
    (want? [] true)
    (want? '() true)
    (want? {} true)
    (want? #{} true)))

(deftest test-simple-atomic-values
  (testing "simple atomic value"
    (is-header :keyword
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/keyword-style} ":keyword"]])
    (is-header 'symbol
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/symbol-style} "symbol"]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/string-style} (str f/dq "some short string" f/dq)]])
    (is-header "line1\nline2\n\nline4"
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/string-style} (str f/dq "line1" f/new-line-string-replacer "line2" f/new-line-string-replacer f/new-line-string-replacer "line4" f/dq)]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (is-header ref
          ["span" {"style" f/string-style} (str f/dq "12345678901234567890" f/string-abbreviation-marker "12345678901234567890" f/dq)])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (is-header ref
          ["span" {"style" f/string-style} (str f/dq "1234" f/new-line-string-replacer "678901234567890" f/string-abbreviation-marker "12345678901234" f/new-line-string-replacer "67890" f/dq)])))))

(deftest test-collections
  (testing "vectors"
    (is-header [1 2 3]
      ["span" {"style" f/general-cljs-land-style}
       "["
       ["span" {"style" f/integer-style} 1] f/spacer
       ["span" {"style" f/integer-style} 2] f/spacer
       ["span" {"style" f/integer-style} 3]
       "]"]))
  (testing "ranges"
    (is-header (range 100)
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
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
           ")"])))))