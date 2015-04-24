(ns devtools.test.format
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll]]
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
    (want? ::auto-namespaced-keyword true)
    (want? :devtools/fully-qualified-keyword true)
    (want? 'symbol true)
    (want? [] true)
    (want? '() true)
    (want? {} true)
    (want? #{} true)))

(deftest bodies
  (testing "these values should not have body"
    (has-body? "some string" false)
    (has-body? 0 false)
    (has-body? 1000 false)
    (has-body? -1000 false)
    (has-body? 0.5 false)
    (has-body? 0.0 false)
    (has-body? -0.5 false)
    (has-body? true false)
    (has-body? false false)
    (has-body? nil false)
    (has-body? #(.-document js/window) false)
    (has-body? :keyword false)
    (has-body? ::auto-namespaced-keyword false)
    (has-body? :devtools/fully-qualified-keyword false)
    (has-body? 'symbol false)
    (has-body? [] false)
    (has-body? '() false)
    (has-body? {} false)
    (has-body? #{} false)
    (has-body? (range f/max-number-body-items) false)))

(deftest test-simple-atomic-values
  (testing "keywords"
    (is-header :keyword
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/keyword-style} ":keyword"]])
    (is-header ::auto-namespaced-keyword
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/keyword-style} ":devtools.test.format/auto-namespaced-keyword"]])
    (is-header :devtools/fully-qualified-keyword
      ["span" {"style" f/general-cljs-land-style}
       ["span" {"style" f/keyword-style} ":devtools/fully-qualified-keyword"]]))
  (testing "symbols"
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
          ["span" {"style" f/general-cljs-land-style}
           ["span" {"style" f/string-style} (str f/dq "12345678901234567890" f/string-abbreviation-marker "12345678901234567890" f/dq)]])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (is-header ref
          ["span" {"style" f/general-cljs-land-style}
           ["span" {"style" f/string-style}
            (str
              f/dq
              "1234" f/new-line-string-replacer "678901234567890"
              f/string-abbreviation-marker
              "12345678901234" f/new-line-string-replacer "67890"
              f/dq)]])
        (is-body ref
          ["ol" {"style" f/standard-ol-style}
           ["li" {"style" f/standard-li-style}
            ["span" {"style" f/string-style}
             (str
               f/dq
               "1234" f/new-line-string-replacer
               "\n6789012345678901234567890123456789012345678901234" f/new-line-string-replacer
               "\n67890"
               f/dq)]]])))))

(deftest test-collections
  (testing "vectors"
    (is-header [1 2 3]
      ["span" {"style" f/general-cljs-land-style}
       "["
       ["span" {"style" f/integer-style} 1] f/spacer
       ["span" {"style" f/integer-style} 2] f/spacer
       ["span" {"style" f/integer-style} 3]
       "]"])
    (is (= 5 f/max-header-elements))
    (is-header [1 2 3 4 5]
      ["span" {"style" f/general-cljs-land-style}
       "["
       (unroll (fn [i] [["span" {"style" f/integer-style} (+ i 1)] f/spacer]) (range 4))
       ["span" {"style" f/integer-style} 5]
       "]"])
    (is-header [1 2 3 4 5 6]
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (is-header ref
          ["span" {"style" f/general-cljs-land-style}
           ["span" {}
            "["
            (unroll (fn [i] [["span" {"style" f/integer-style} (+ i 1)] f/spacer]) (range 5))
            f/more-marker
            "]"]])
        (has-body? ref true)
        (is-body ref
          ["ol" {"style" f/standard-ol-style}
           (unroll (fn [i] [["li" {"style" f/standard-li-style}
                             ["span" {"style" f/index-style} i f/line-index-separator]
                             f/spacer
                             ["span" {"style" f/general-cljs-land-style}
                              ["span" {"style" f/integer-style} (+ i 1)]]]]) (range 6))]))))
  (testing "ranges"
    (is (> 10 f/max-header-elements))
    (is-header (range 10)
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" f/general-cljs-land-style}
           ["span" {}
            "("
            (unroll (fn [i] [["span" {"style" f/integer-style} i] f/spacer]) (range f/max-header-elements))
            f/more-marker
            ")"]])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ f/max-number-body-items 1))
      ["span" {"style" f/general-cljs-land-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (f/surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" f/general-cljs-land-style}
           ["span" {}
            "("
            (unroll (fn [i] [["span" {"style" f/integer-style} i] f/spacer]) (range f/max-header-elements))
            f/more-marker
            ")"]])
        (is-body ref
          ["ol" {"style" f/standard-ol-style}
           (unroll (fn [i] [["li" {"style" f/standard-li-style}
                             ["span" {"style" f/index-style} i f/line-index-separator]
                             f/spacer
                             ["span" {"style" f/general-cljs-land-style}
                              ["span" {"style" f/integer-style} i]]]]) (range f/max-number-body-items))
           ["li" {"style" f/standard-li-style}
            ["object" {"object" "##REF##"}]]]
          (fn [ref]
            (is (f/surrogate? ref))
            (has-body? ref true)
            (is-header ref
              ["span" {"style" f/general-cljs-land-style} f/body-items-more-label])
            (is-body ref
              ["ol" {"style" f/standard-ol-no-margin-style}
               (unroll (fn [i] [["li" {"style" f/standard-li-no-margin-style}
                                 ["span" {"style" f/index-style} f/max-number-body-items f/line-index-separator]
                                 f/spacer
                                 ["span" {"style" f/general-cljs-land-style}
                                  ["span" {"style" f/integer-style} (+ i f/max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        ["span" {"style" f/general-cljs-land-style}
         "["
         ["span" {"style" f/integer-style} 1]
         f/spacer
         "["
         ["span" {"style" f/integer-style} 2]
         f/spacer
         "["
         ["span" {"style" f/integer-style} 3]
         f/spacer
         ["object" {"object" "##REF##"}]
         "]"
         "]"
         "]"]))))