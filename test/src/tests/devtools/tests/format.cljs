(ns devtools.tests.format
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str]])                                                        ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll remove-empty-styles pref-str]]
            [devtools.format :refer [surrogate? header-api-call has-body-api-call body-api-call]]
            [devtools.prefs :refer [default-prefs merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.format :as f]
            [devtools.utils.batteries :as b]))

(def REF ["object" {"object" "##REF##"
                    "config" "##CONFIG##"}])

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
    (want? (SimpleType. "some-value") false)
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
    (has-body? (range :max-number-body-items) false)))

(deftest test-simple-atomic-values
  (testing "keywords"
    (is-header :keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :keyword-style} ":keyword"]]])
    (is-header ::auto-namespaced-keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :keyword-style} ":devtools.tests.format/auto-namespaced-keyword"]]])
    (is-header :devtools/fully-qualified-keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :keyword-style} ":devtools/fully-qualified-keyword"]]]))
  (testing "symbols"
    (is-header 'symbol
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :symbol-style} "symbol"]]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :string-style} (str :dq "some short string" :dq)]]])
    (is-header "line1\nline2\n\nline4"
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :string-style} (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :string-style} (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :string-style}
           (str
             :dq
             "1234" :new-line-string-replacer "678901234567890"
             :string-abbreviation-marker
             "12345678901234" :new-line-string-replacer "67890"
             :dq)])
        (is-body ref
          ["span" {"style" :expanded-string-style}
           (str
             "1234" :new-line-string-replacer
             "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
             "\n67890")])))))

(deftest test-collections
  (testing "vectors"
    (set-pref! :seqables-always-expandable false)
    (is-header [1 2 3]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        "["
        ["span" {"style" :integer-style} 1] :spacer
        ["span" {"style" :integer-style} 2] :spacer
        ["span" {"style" :integer-style} 3]
        "]"]])
    (is (= 5 :max-header-elements))
    (is-header [1 2 3 4 5]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        "["
        (unroll (fn [i] [["span" {"style" :integer-style} (+ i 1)] :spacer]) (range 4))
        ["span" {"style" :integer-style} 5]
        "]"]])
    (set-prefs! default-prefs)
    (is-header [1 2 3]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]])
    (is-header [1 2 3 4 5 6]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" {"style" :header-style}
           "["
           (unroll (fn [i] [["span" {"style" :integer-style} (+ i 1)] :spacer]) (range 5))
           :more-marker
           "]"])
        (has-body? ref true)
        (is-body ref
          ["span" {"style" :body-style}
           ["ol" {"style" :standard-ol-style}
            (unroll (fn [i] [["li" {"style" :standard-li-style}
                              ["span" {"style" :index-style} i :line-index-separator]
                              ["span" {"style" :item-style}
                               ["span" {"style" :integer-style} (+ i 1)]]]]) (range 6))]]))))
  (testing "ranges"
    (is (> 10 :max-header-elements))
    (is-header (range 10)
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :header-style}
           "("
           (unroll (fn [i] [["span" {"style" :integer-style} i] :spacer]) (range :max-header-elements))
           :more-marker
           ")"])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ :max-number-body-items 1))
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :header-style}
           "("
           (unroll (fn [i] [["span" {"style" :integer-style} i] :spacer]) (range :max-header-elements))
           :more-marker
           ")"])
        (is-body ref
          ["span" {"style" :body-style}
           ["ol" {"style" :standard-ol-style}
            (unroll (fn [i] [["li" {"style" :standard-li-style}
                              ["span" {"style" :index-style} i :line-index-separator]
                              ["span" {"style" :item-style}
                               ["span" {"style" :integer-style} i]]]]) (range :max-number-body-items))
            ["li" {"style" :standard-li-style}
             REF]]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              ["span" {"style" :body-items-more-label-style}
               :body-items-more-label])
            (is-body ref
              ["ol" {"style" :standard-ol-no-margin-style}
               (unroll (fn [i] [["li" {"style" :standard-li-no-margin-style}
                                 ["span" {"style" :index-style} :max-number-body-items :line-index-separator]
                                 ["span" {"style" :item-style}
                                  ["span" {"style" :integer-style} (+ i :max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        ["span" {"style" :cljs-style}
         ["span" {"style" :header-style}
          "[" ["span" {"style" :integer-style} 1] " "
          "[" ["span" {"style" :integer-style} 2] " "
          REF
          "]"
          "]"]]
        (fn [ref]
          (is (surrogate? ref))
          (has-body? ref true)
          (is-header ref
            ["span" {"style" :header-style}
             "[" :more-marker "]"]))))))

(deftest test-deftype
  (testing "simple deftype"
    (let [type-instance (SimpleType. "some-value")]
      (is-header type-instance
        ["span" {"style" :cljs-style}
         ["span" {"style" :header-style}
          REF]]
        (fn [ref config]
          (is (:prevent-recursion config))
          (is (not (surrogate? ref))))))))

(deftest test-handlers
  (let [handled-output (clj->js (remove-empty-styles ["span" {"style" (pref :cljs-style)}
                                                      ["span" {"style" (pref :header-style)}
                                                       ["span" {"style" (pref :keyword-style)} ":handled"]]]))]
    (testing "header pre-handler"
      (set-pref! :header-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled)))
      (is (js-equals (header-api-call ["cljs-value"]) handled-output))
      (is (not (js-equals (header-api-call ["non-matching-cljs-value"]) handled-output)))
      (set-pref! :header-pre-handler (fn [value] (if (= value "javascript-value") :handled)))
      (is (js-equals (header-api-call "javascript-value") handled-output))
      (is (not (js-equals (header-api-call "not-matching-javascript-value") handled-output)))
      (set-prefs! default-prefs))
    (testing "header post-handler"
      (set-pref! :header-post-handler (fn [_value] "always-rewrite"))
      (is (= (header-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (header-api-call "javascript-value") "always-rewrite"))
      (set-prefs! default-prefs))
    (testing "has-body pre-handler"
      (set-pref! :has-body-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled)))
      (is (= (has-body-api-call ["cljs-value"]) false))
      (is (= (has-body-api-call :non-matching-cljs-value) nil))
      (set-pref! :has-body-pre-handler (fn [value] (if (= value "javascript-value") :handled)))
      (is (= (has-body-api-call "javascript-value") false))
      (is (= (has-body-api-call "not-matching-javascript-value") nil))
      (set-prefs! default-prefs))
    (testing "has-body post-handler"
      (set-pref! :has-body-post-handler (fn [_value] "always-rewrite"))
      (is (= (has-body-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (has-body-api-call "javascript-value") "always-rewrite"))
      (set-prefs! default-prefs))
    (testing "body pre-handler"
      (set-pref! :body-pre-handler (fn [value] (if (= value ["cljs-value"]) ["handled-cljs-value"])))
      (is (= (body-api-call ["cljs-value"]) nil))
      (is (= (body-api-call ["non-matching-cljs-value"]) nil))
      (set-prefs! default-prefs))
    (testing "header post-handler"
      (set-pref! :body-post-handler (fn [_value] "always-rewrite"))
      (is (= (body-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (body-api-call "javascript-value") "always-rewrite"))
      (set-prefs! default-prefs))))

(deftest test-meta
  (testing "meta is disabled"
    (set-pref! :print-meta-data false)
    (is-header (with-meta {} :meta)
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        "{" "}"]])
    (set-prefs! default-prefs))
  (testing "simple meta"
    (is-header (with-meta {} :meta)
      ["span" {"style" :cljs-style}
       ["span" {"style" :meta-wrapper-style}
        ["span" {"style" :header-style} "{" "}"]
        ["span" {} REF]]]
      (fn [ref]
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :meta-style} "meta"])
        (is-body ref
          ["span" {"style" :meta-body-style}
           ["span" {"style" :header-style}
            ["span" {"style" :keyword-style} ":meta"]]])))))

(deftest test-sequables
  (testing "min-sequable-count-for-expansion"
    (set-pref! :max-header-elements 100)
    (set-pref! :seqables-always-expandable true)
    (set-pref! :min-sequable-count-for-expansion 3)
    (is-header [1 2]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        "["
        ["span" {"style" :integer-style} 1]
        " "
        ["span" {"style" :integer-style} 2]
        "]"]])
    (is-header [1 2 3]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :header-style}
           "["
           ["span" {"style" :integer-style} 1]
           " "
           ["span" {"style" :integer-style} 2]
           " "
           ["span" {"style" :integer-style} 3]
           "]"])))
    (set-pref! :min-sequable-count-for-expansion 4)
    (is-header [1 2 3]
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        "["
        ["span" {"style" :integer-style} 1]
        " "
        ["span" {"style" :integer-style} 2]
        " "
        ["span" {"style" :integer-style} 3]
        "]"]])
    (set-prefs! default-prefs)))

(deftest test-circular-data
  (testing "circulare data structure"
    (let [circular-ds (atom nil)]
      (reset! circular-ds circular-ds)
      (is-header circular-ds
        ["span" {"style" :cljs-style}
         ["span" {"style" :header-style}
          "#object [cljs.core.Atom "
          "{"
          ["span" {"style" :keyword-style} ":val"] " "
          ["span" {"style" :circular-reference-wrapper-style}
           ["span" {"style" :circular-reference-symbol-style} :circular-reference-symbol]
           "#object [cljs.core.Atom "
           "{"
           ["span" {"style" :keyword-style} ":val"] " "
           ["span" {"style" :circular-reference-wrapper-style}
            ["span" {"style" :circular-reference-symbol-style} :circular-reference-symbol]
            "#object [cljs.core.Atom "
            REF
            "]"]
           "}"
           "]"]
          "}"
          "]"]]))))

(deftest test-function-formatting
  (testing "cljs-function?"
    (testing "these should NOT be recognized as cljs functions"
      (are [f] (not (f/cljs-function? f))
        b/simplest-fn))
    (testing "these should be recognized as cljs functions"
      (are [f] (f/cljs-function? f)
        b/minimal-fn
        b/cljs-lambda-multi-arity
        b/clsj-fn-with-fancy-name#$%!?
        b/cljs-fn-var
        b/cljs-fn-multi-arity-var
        b/cljs-fn-multi-arity
        b/cljs-fn-with-vec-destructuring
        b/inst-type-ifn0
        b/inst-type-ifn1
        b/inst-type-ifn2
        b/inst-type-ifn2va
        b/inst-type-ifn4va))
    (testing "these should be recognized as cljs functions"
      (set-pref! :disable-cljs-fn-formatting true)
      (are [f] (not (f/cljs-function? f))
        b/minimal-fn
        b/clsj-fn-with-fancy-name#$%!?
        b/cljs-fn-var
        b/cljs-fn-multi-arity-var
        b/cljs-fn-multi-arity
        b/cljs-fn-with-vec-destructuring
        b/inst-type-ifn0
        b/inst-type-ifn1
        b/inst-type-ifn2
        b/inst-type-ifn2va
        b/inst-type-ifn4va)
      (set-prefs! default-prefs)))
  (testing "minimal function formatting"
    (is-header b/minimal-fn
      ["span"
       {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :fn-header-style}
           ["span" {"style" :fn-prefix-style}
            ["span" {"style" :fn-lambda-symbol-style} :fn-lambda-symbol]]
           ["span" {"style" :fn-args-style} (pref-str :args-open-symbol :args-close-symbol)]])
        (is-body ref
          ["span" {"style" :body-style}
           ["ol" {"style" :standard-ol-no-margin-style}
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-native-symbol-style} :fn-native-symbol]
             REF]]]))))
  (testing "cljs-lambda-multi-arity function formatting"
    (is-header b/cljs-lambda-multi-arity
      ["span"
       {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :fn-header-style}
           ["span" {"style" :fn-prefix-style}
            ["span" {"style" :fn-lambda-symbol-style} :fn-lambda-symbol]]
           ["span" {"style" :fn-args-style} (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]])
        (is-body ref
          ["span" {"style" :body-style}
           ["ol" {"style" :standard-ol-no-margin-style}
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-lambda-symbol-style} :fn-lambda-symbol]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-lambda-symbol-style} :fn-lambda-symbol]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "a b" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-lambda-symbol-style} :fn-lambda-symbol]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "c d e f" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-native-symbol-style} :fn-native-symbol]
             REF]]]))))
  (testing "cljs-fn-multi-arity-var function formatting"
    (is-header b/cljs-fn-multi-arity-var
      ["span"
       {"style" :cljs-style}
       ["span" {"style" :header-style}
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" {"style" :fn-header-style}
           ["span" {"style" :fn-prefix-style}
            ["span" {"style" :fn-symbol-style} :fn-symbol]
            ["span" {"style" :fn-name-style} "cljs-fn-multi-arity-var"]]
           ["span" {"style" :fn-args-style} (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]])
        (is-body ref
          ["span" {"style" :body-style}
           ["ol" {"style" :standard-ol-no-margin-style}
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-symbol-style} :fn-symbol]
               ["span" {"style" :fn-name-style} "cljs-fn-multi-arity-var"]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "a1" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-symbol-style} :fn-symbol]
               ["span" {"style" :fn-name-style} "cljs-fn-multi-arity-var"]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "a2-1 a2-2" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-symbol-style} :fn-symbol]
               ["span" {"style" :fn-name-style} "cljs-fn-multi-arity-var"]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "a3-1 a3-2 a3-3 a3-4" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-multi-arity-args-indent-style}
              ["span" {"style" :fn-prefix-style}
               ["span" {"style" :fn-symbol-style} :fn-symbol]
               ["span" {"style" :fn-name-style} "cljs-fn-multi-arity-var"]]]
             ["span" {"style" :fn-args-style} (pref-str :args-open-symbol "va1 va2 & rest" :args-close-symbol)]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-ns-symbol-style} :fn-ns-symbol]
             ["span" {"style" :fn-ns-name-style} "devtools.utils.batteries"]]
            ["li" {"style" :aligned-li-style}
             ["span" {"style" :fn-native-symbol-style} :fn-native-symbol]
             REF]]])))))
