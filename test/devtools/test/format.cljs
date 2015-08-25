(ns devtools.test.format
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str]]) ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll]]
            [devtools.format :refer [surrogate?]]))

(deftype SimpleType [some-field])

(deftest test-wants
  (testing "these simple values SHOULD NOT be processed by our custom formatter"
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
    (want? (SimpleType. "some-value") #js {"prevent-recursion" true} false)
    (want? #(.-document js/window) false))
  (testing "these values SHOULD be processed by our custom formatter"
    (want? :keyword true)
    (want? ::auto-namespaced-keyword true)
    (want? :devtools/fully-qualified-keyword true)
    (want? 'symbol true)
    (want? [] true)
    (want? '() true)
    (want? {} true)
    (want? #{} true)
    (want? (SimpleType. "some-value") true)
    (want? (range :max-number-body-items) true)))

(deftest test-bodies
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
    (has-body? (SimpleType. "some-value") false)
    (has-body? (SimpleType. "some-value") #js {"prevent-recursion" true} false)
    (has-body? (range :max-number-body-items) false)))

(deftest test-simple-atomic-values
  (testing "keywords"
    (is-header :keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :keyword-style} ":keyword"]])
    (is-header ::auto-namespaced-keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :keyword-style} ":devtools.test.format/auto-namespaced-keyword"]])
    (is-header :devtools/fully-qualified-keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :keyword-style} ":devtools/fully-qualified-keyword"]]))
  (testing "symbols"
    (is-header 'symbol
      ["span" {"style" :cljs-style}
       ["span" {"style" :symbol-style} "symbol"]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      ["span" {"style" :cljs-style}
       ["span" {"style" :string-style} (str :dq "some short string" :dq)]])
    (is-header "line1\nline2\n\nline4"
      ["span" {"style" :cljs-style}
       ["span" {"style" :string-style} (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      ["span" {"style" :cljs-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :cljs-style}
           ["span" {"style" :string-style} (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)]])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      ["span" {"style" :cljs-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :cljs-style}
           ["span" {"style" :string-style}
            (str
              :dq
              "1234" :new-line-string-replacer "678901234567890"
              :string-abbreviation-marker
              "12345678901234" :new-line-string-replacer "67890"
              :dq)]])
        (is-body ref
          ["ol" {"style" :standard-ol-style}
           ["li" {"style" :standard-li-style}
            ["span" {"style" :string-style}
             (str
               :dq
               "1234" :new-line-string-replacer
               "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
               "\n67890"
               :dq)]]])))))

(deftest test-collections
  (testing "vectors"
    (is-header [1 2 3]
      ["span" {"style" :cljs-style}
       "["
       ["span" {"style" :integer-style} 1] :spacer
       ["span" {"style" :integer-style} 2] :spacer
       ["span" {"style" :integer-style} 3]
       "]"])
    (is (= 5 :max-header-elements))
    (is-header [1 2 3 4 5]
      ["span" {"style" :cljs-style}
       "["
       (unroll (fn [i] [["span" {"style" :integer-style} (+ i 1)] :spacer]) (range 4))
       ["span" {"style" :integer-style} 5]
       "]"])
    (is-header [1 2 3 4 5 6]
      ["span" {"style" :cljs-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :cljs-style}
           ["span" {}
            "["
            (unroll (fn [i] [["span" {"style" :integer-style} (+ i 1)] :spacer]) (range 5))
            :more-marker
            "]"]])
        (has-body? ref true)
        (is-body ref
          ["ol" {"style" :standard-ol-style}
           (unroll (fn [i] [["li" {"style" :standard-li-style}
                             ["span" {"style" :index-style} i :line-index-separator]
                             :spacer
                             ["span" {"style" :cljs-style}
                              ["span" {"style" :integer-style} (+ i 1)]]]]) (range 6))]))))
  (testing "ranges"
    (is (> 10 :max-header-elements))
    (is-header (range 10)
      ["span" {"style" :cljs-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :cljs-style}
           ["span" {}
            "("
            (unroll (fn [i] [["span" {"style" :integer-style} i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ :max-number-body-items 1))
      ["span" {"style" :cljs-style}
       ["object" {"object" "##REF##"}]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :cljs-style}
           ["span" {}
            "("
            (unroll (fn [i] [["span" {"style" :integer-style} i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])
        (is-body ref
          ["ol" {"style" :standard-ol-style}
           (unroll (fn [i] [["li" {"style" :standard-li-style}
                             ["span" {"style" :index-style} i :line-index-separator]
                             :spacer
                             ["span" {"style" :cljs-style}
                              ["span" {"style" :integer-style} i]]]]) (range :max-number-body-items))
           ["li" {"style" :standard-li-style}
            ["object" {"object" "##REF##"}]]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              ["span" {"style" :cljs-style} :body-items-more-label])
            (is-body ref
              ["ol" {"style" :standard-ol-no-margin-style}
               (unroll (fn [i] [["li" {"style" :standard-li-no-margin-style}
                                 ["span" {"style" :index-style} :max-number-body-items :line-index-separator]
                                 :spacer
                                 ["span" {"style" :cljs-style}
                                  ["span" {"style" :integer-style} (+ i :max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        ["span" {"style" :cljs-style}
         "["
         ["span" {"style" :integer-style} 1]
         :spacer
         "["
         ["span" {"style" :integer-style} 2]
         :spacer
         ["object" {"object" "##REF##"}]
         "]"
         "]"]))))

(deftest test-deftype
  (testing "simple deftype"
    (let [type-instance (SimpleType. "some-value")]
      (is-header type-instance
        ["span" {"style" :cljs-style}
         "#object["
         "devtools.test.format.SimpleType"
         "]"]))))
