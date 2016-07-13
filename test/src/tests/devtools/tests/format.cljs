(ns devtools.tests.format
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str want?]])                                                  ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.tests.style :as style]
            [devtools.utils.test :refer [js-equals is-header is-body has-body? unroll remove-empty-styles pref-str]]
            [devtools.format :refer [surrogate? header-api-call has-body-api-call body-api-call]]
            [devtools.prefs :refer [default-prefs merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.format :as f]
            [devtools.utils.batteries :as b :refer [REF]]))

(deftest test-wants
  (testing "these simple values SHOULD NOT be processed by our custom formatter"
    (are [v] (want? v false)
      "some string"
      0
      1000
      -1000
      0.5
      0.0
      -0.5
      true
      false
      nil
      (goog.date.Interval.)                                                                                                   ; this type was not extended to support IPrintWithWriter or IFormat
      #js {}
      #js []))
  (testing "these values SHOULD be processed by our custom formatter"
    (are [v] (want? v true)
      :keyword
      ::auto-namespaced-keyword
      :devtools/fully-qualified-keyword
      'symbol
      []
      '()
      {}
      #{}
      #(.-document js/window)
      (b/SimpleType. "some-value")
      (range :max-number-body-items)
      (goog.date.Date.)                                                                                                       ; see extend-protocol IPrintWithWriter for goog.date.Date in batteries
      (goog.date.DateTime.)                                                                                                   ; inherits from goog.date.Date
      ; TODO!
      ;(goog.Promise.)                                                                                                         ; see extend-protocol IFormat for goog.Promise in batteries
      (b/get-raw-js-obj-implementing-iformat)
      (b/get-raw-js-obj-implementing-iprintwithwriter))))

(deftest test-bodies
  (testing "these values should not have body"
    (are [v] (has-body? v false)
      "some string"
      0
      1000
      -1000
      0.5
      0.0
      -0.5
      true
      false
      nil
      #(.-document js/window)
      :keyword
      ::auto-namespaced-keyword
      :devtools/fully-qualified-keyword
      'symbol
      []
      '()
      {}
      #{}
      (b/SimpleType. "some-value")
      (range :max-number-body-items))))

(deftest test-simple-atomic-values
  (testing "keywords"
    (is-header :keyword
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/keyword ":keyword"]]])
    (is-header ::auto-namespaced-keyword
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/keyword ":devtools.tests.format/auto-namespaced-keyword"]]])
    (is-header :devtools/fully-qualified-keyword
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/keyword ":devtools/fully-qualified-keyword"]]]))
  (testing "symbols"
    (is-header 'symbol
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/symbol "symbol"]]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/string (str :dq "some short string" :dq)]]])
    (is-header "line1\nline2\n\nline4"
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/string (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" ::style/string (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" ::style/string
           (str
             :dq
             "1234" :new-line-string-replacer "678901234567890"
             :string-abbreviation-marker
             "12345678901234" :new-line-string-replacer "67890"
             :dq)])
        (is-body ref
          ["span" ::style/expanded-string
           (str
             "1234" :new-line-string-replacer
             "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
             "\n67890")])))))

(deftest test-collections
  (testing "vectors"
    (set-pref! :seqables-always-expandable false)
    (is-header [1 2 3]
      ["span" ::style/cljs
       ["span" ::style/header
        "["
        ["span" ::style/integer 1] :spacer
        ["span" ::style/integer 2] :spacer
        ["span" ::style/integer 3]
        "]"]])
    (is (= 5 :max-header-elements))
    (is-header [1 2 3 4 5]
      ["span" ::style/cljs
       ["span" ::style/header
        "["
        (unroll (fn [i] [["span" ::style/integer (+ i 1)] :spacer]) (range 4))
        ["span" ::style/integer 5]
        "]"]])
    (set-prefs! default-prefs)
    (is-header [1 2 3]
      ["span" ::style/cljs
       ["span" ::style/header
        REF]])
    (is-header [1 2 3 4 5 6]
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          ["span" ::style/header
           "["
           (unroll (fn [i] [["span" ::style/integer (+ i 1)] :spacer]) (range 5))
           :more-marker
           "]"])
        (has-body? ref true)
        (is-body ref
          ["span" ::style/body
           ["ol" ::style/standard-ol
            (unroll (fn [i] [["li" ::style/standard-li
                              ["span" ::style/index i :line-index-separator]
                              ["span" ::style/item
                               ["span" ::style/integer (+ i 1)]]]]) (range 6))]]))))
  (testing "ranges"
    (is (> 10 :max-header-elements))
    (is-header (range 10)
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/header
           "("
           (unroll (fn [i] [["span" ::style/integer i] :spacer]) (range :max-header-elements))
           :more-marker
           ")"])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ :max-number-body-items 1))
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/header
           "("
           (unroll (fn [i] [["span" ::style/integer i] :spacer]) (range :max-header-elements))
           :more-marker
           ")"])
        (is-body ref
          ["span" ::style/body
           ["ol" ::style/standard-ol
            (unroll (fn [i] [["li" ::style/standard-li
                              ["span" ::style/index i :line-index-separator]
                              ["span" ::style/item
                               ["span" ::style/integer i]]]]) (range :max-number-body-items))
            ["li" ::style/standard-li
             REF]]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              ["span" ::style/body-items-more-label
               :body-items-more-label])
            (is-body ref
              ["ol" ::style/standard-ol-no-margin
               (unroll (fn [i] [["li" ::style/standard-li-no-margin
                                 ["span" ::style/index :max-number-body-items :line-index-separator]
                                 ["span" ::style/item
                                  ["span" ::style/integer (+ i :max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        ["span" ::style/cljs
         ["span" ::style/header
          "[" ["span" ::style/integer 1] " "
          "[" ["span" ::style/integer 2] " "
          REF
          "]"
          "]"]]
        (fn [ref]
          (is (surrogate? ref))
          (has-body? ref true)
          (is-header ref
            ["span" ::style/header
             "[" :more-marker "]"]))))))

(deftest test-deftype
  (testing "simple deftype"
    (let [type-instance (b/SimpleType. "some-value")]
      (is-header type-instance
        ["span" ::style/cljs
         ["span" ::style/header
          REF]]))))                                                                                                           ; TODO!

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
      ["span" ::style/cljs
       ["span" ::style/header
        "{" "}"]])
    (set-prefs! default-prefs))
  (testing "simple meta"
    (is-header (with-meta {} :meta)
      ["span" ::style/cljs
       ["span" ::style/header
        ["span" ::style/meta-wrapper
         "{" "}" ["span" ::style/meta-reference-style REF]]]]
      (fn [ref]
        (has-body? ref true)
        (is-header ref
          ["span" ::style/meta "meta"])
        (is-body ref
          ["span" ::style/meta-body
           ["span" ::style/header
            ["span" ::style/keyword ":meta"]]])))))

(deftest test-sequables
  (testing "min-sequable-count-for-expansion"
    (set-pref! :max-header-elements 100)
    (set-pref! :seqables-always-expandable true)
    (set-pref! :min-sequable-count-for-expansion 3)
    (is-header [1 2]
      ["span" ::style/cljs
       ["span" ::style/header
        "["
        ["span" ::style/integer 1]
        " "
        ["span" ::style/integer 2]
        "]"]])
    (is-header [1 2 3]
      ["span" ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/header
           "["
           ["span" ::style/integer 1]
           " "
           ["span" ::style/integer 2]
           " "
           ["span" ::style/integer 3]
           "]"])))
    (set-pref! :min-sequable-count-for-expansion 4)
    (is-header [1 2 3]
      ["span" ::style/cljs
       ["span" ::style/header
        "["
        ["span" ::style/integer 1]
        " "
        ["span" ::style/integer 2]
        " "
        ["span" ::style/integer 3]
        "]"]])
    (set-prefs! default-prefs)))

(deftest test-circular-data
  (testing "circulare data structure"
    (let [circular-ds (atom nil)]
      (reset! circular-ds circular-ds)
      (is-header circular-ds
        ["span" ::style/cljs
         ["span" ::style/header
          REF]]))))                                                                                                           ; TODO

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
       ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/fn-header
           ["span" ::style/fn-prefix
            ["span" ::style/fn-lambda-symbol :fn-lambda-symbol]]
           ["span" ::style/fn-args (pref-str :args-open-symbol :args-close-symbol)]])
        (is-body ref
          ["span" ::style/body
           ["ol" ::style/standard-ol-no-margin
            ["li" ::style/aligned-li
             ["span" ::style/fn-native-symbol :fn-native-symbol]
             REF]]]))))
  (testing "cljs-lambda-multi-arity function formatting"
    (is-header b/cljs-lambda-multi-arity
      ["span"
       ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/fn-header
           ["span" ::style/fn-prefix
            ["span" ::style/fn-lambda-symbol :fn-lambda-symbol]]
           ["span" ::style/fn-args (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]])
        (is-body ref
          ["span" ::style/body
           ["ol" ::style/standard-ol-no-margin
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-lambda-symbol :fn-lambda-symbol]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-lambda-symbol :fn-lambda-symbol]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "a b" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-lambda-symbol :fn-lambda-symbol]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "c d e f" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-native-symbol :fn-native-symbol]
             REF]]]))))
  (testing "cljs-fn-multi-arity-var function formatting"
    (is-header b/cljs-fn-multi-arity-var
      ["span"
       ::style/cljs
       ["span" ::style/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          ["span" ::style/fn-header
           ["span" ::style/fn-prefix
            ["span" ::style/fn-symbol :fn-symbol]
            ["span" ::style/fn-name "cljs-fn-multi-arity-var"]]
           ["span" ::style/fn-args (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]])
        (is-body ref
          ["span" ::style/body
           ["ol" ::style/standard-ol-no-margin
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-symbol :fn-symbol]
               ["span" ::style/fn-name "cljs-fn-multi-arity-var"]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "a1" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-symbol :fn-symbol]
               ["span" ::style/fn-name "cljs-fn-multi-arity-var"]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "a2-1 a2-2" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-symbol :fn-symbol]
               ["span" ::style/fn-name "cljs-fn-multi-arity-var"]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "a3-1 a3-2 a3-3 a3-4" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-multi-arity-args-indent
              ["span" ::style/fn-prefix
               ["span" ::style/fn-symbol :fn-symbol]
               ["span" ::style/fn-name "cljs-fn-multi-arity-var"]]]
             ["span" ::style/fn-args (pref-str :args-open-symbol "va1 va2 & rest" :args-close-symbol)]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-ns-symbol :fn-ns-symbol]
             ["span" ::style/fn-ns-name "devtools.utils.batteries"]]
            ["li" ::style/aligned-li
             ["span" ::style/fn-native-symbol :fn-native-symbol]
             REF]]])))))

(deftest test-alt-printer-impl
  (testing "wrapping IPrintWithWriter products as references if needed (issue #21)"                                           ; https://github.com/binaryage/cljs-devtools/issues/21
    (let [date-map {:date (goog.date.Date. 2016 6 1)}]                                                                        ; see extend-protocol IPrintWithWriter for goog.date.Date in batteries
      (is-header date-map
        ["span" ::style/cljs
         ["span" ::style/header
          "{"
          ["span" ::style/keyword ":date"]
          :spacer
          "#gdate "
          REF
          REF
          REF
          "}"]]
        (fn [ref]
          (is-header ref
            ["span" ::style/cljs
             ["span" ::style/header
              REF]]
            (fn [ref]
              (has-body? ref true)
              (is-header ref
                ["span" ::style/header
                 "["
                 ["span" ::style/integer 2016]
                 :spacer
                 ["span" ::style/integer 6]
                 :spacer
                 ["span" ::style/integer 1]
                 "]"]))))
        (fn [ref]
          (is-header ref
            ["span" ::style/cljs
             ["span" ::style/header
              "#js ["
              ["span" ::style/string "\"test-array\""]
              "]"]]))
        (fn [ref]
          (is-header ref
            ["span" ::style/cljs
             ["span" ::style/header
              "#js "
              "{"
              ["span" ::style/keyword ":some-key"]
              :spacer
              ["span" ::style/string "\"test-js-obj\""]
              "}"]]))))))
