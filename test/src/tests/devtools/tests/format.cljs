(ns devtools.tests.format
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str want? with-prefs]])                                       ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.pseudo.tag :as tag]
            [devtools.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                         remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.formatters.helpers :refer [cljs-function?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.utils.batteries :as b :refer [REF NATIVE-REF]]))

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
      [::tag/cljs-land
       [::tag/header
        [::tag/keyword ":keyword"]]])
    (is-header ::auto-namespaced-keyword
      [::tag/cljs-land
       [::tag/header
        [::tag/keyword ":devtools.tests.format/auto-namespaced-keyword"]]])
    (is-header :devtools/fully-qualified-keyword
      [::tag/cljs-land
       [::tag/header
        [::tag/keyword ":devtools/fully-qualified-keyword"]]]))
  (testing "symbols"
    (is-header 'symbol
      [::tag/cljs-land
       [::tag/header
        [::tag/symbol "symbol"]]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      [::tag/cljs-land
       [::tag/header
        [::tag/string (str :dq "some short string" :dq)]]])
    (is-header "line1\nline2\n\nline4"
      [::tag/cljs-land
       [::tag/header
        [::tag/string (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/string (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)]]])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/string
             (str
               :dq
               "1234" :new-line-string-replacer "678901234567890"
               :string-abbreviation-marker
               "12345678901234" :new-line-string-replacer "67890"
               :dq)]]])
        (is-body ref
          [::tag/expanded-string
           (str
             "1234" :new-line-string-replacer
             "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
             "\n67890")])))))

(deftest test-collections
  (testing "vectors"
    (is (< 4 :max-header-elements))
    (is (> 6 :min-expandable-sequable-count))
    (is-header [1 2 3]
      [::tag/cljs-land
       [::tag/header
        "["
        [::tag/integer 1] :spacer
        [::tag/integer 2] :spacer
        [::tag/integer 3]
        "]"]])
    (is-header [1 2 3 4 5]
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            "["
            (unroll (fn [i] [[::tag/integer (+ i 1)] :spacer]) (range 4))
            [::tag/integer 5]
            "]"]]))))
  (testing "ranges"
    (is (> 10 :max-header-elements))
    (is-header (range 10)
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            "("
            (unroll (fn [i] [[::tag/integer i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ :max-number-body-items 1))
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            "("
            (unroll (fn [i] [[::tag/integer i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])
        (is-body ref
          [::tag/body
           [::tag/standard-ol
            (unroll (fn [i] [[::tag/standard-li
                              [::tag/index i :line-index-separator]
                              [::tag/item
                               [::tag/integer i]]]]) (range :max-number-body-items))
            [::tag/standard-li
             REF]]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              [::tag/expandable
               [::tag/expandable-inner
                [::tag/body-items-more
                 :body-items-more-label]]])
            (is-body ref
              [::tag/standard-ol-no-margin
               (unroll (fn [i] [[::tag/standard-li-no-margin
                                 [::tag/index :max-number-body-items :line-index-separator]
                                 [::tag/item
                                  [::tag/integer (+ i :max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        [::tag/cljs-land
         [::tag/header
          "[" [::tag/integer 1] " "
          "[" [::tag/integer 2] " "
          REF
          "]"
          "]"]]
        (fn [ref]
          (is (surrogate? ref))
          (has-body? ref true)
          (is-header ref
            [::tag/expandable
             [::tag/expandable-inner
              "[" :more-marker "]"]]))))))

#_(deftest test-deftype
    (testing "simple deftype"
      (let [type-instance (b/SimpleType. "some-value")]
        (is-header type-instance
          [::tag/cljs-land
           [::tag/header
            REF]]))))                                                                                                         ; TODO!

(deftest test-handlers
  (let [handled-output (clj->js (remove-empty-styles ["span" {"style" (pref :cljs-land-style)}
                                                      ["span" {"style" (pref :header-style)}
                                                       ["span" {"style" (pref :keyword-style)} ":handled"]]]))]
    (testing "header pre-handler"
      (set-pref! :header-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled)))
      (is (js-equals (header-api-call ["cljs-value"]) handled-output))
      (is (not (js-equals (header-api-call ["non-matching-cljs-value"]) handled-output)))
      (set-pref! :header-pre-handler (fn [value] (if (= value "javascript-value") :handled)))
      (is (js-equals (header-api-call "javascript-value") handled-output))
      (is (not (js-equals (header-api-call "not-matching-javascript-value") handled-output)))
      (reset-prefs-to-defaults!))
    (testing "header post-handler"
      (set-pref! :header-post-handler (fn [_value] "always-rewrite"))
      (is (= (header-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (header-api-call "javascript-value") "always-rewrite"))
      (reset-prefs-to-defaults!))
    (testing "has-body pre-handler"
      (set-pref! :has-body-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled)))
      (is (= (has-body-api-call ["cljs-value"]) false))
      (is (= (has-body-api-call :non-matching-cljs-value) nil))
      (set-pref! :has-body-pre-handler (fn [value] (if (= value "javascript-value") :handled)))
      (is (= (has-body-api-call "javascript-value") false))
      (is (= (has-body-api-call "not-matching-javascript-value") nil))
      (reset-prefs-to-defaults!))
    (testing "has-body post-handler"
      (set-pref! :has-body-post-handler (fn [_value] "always-rewrite"))
      (is (= (has-body-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (has-body-api-call "javascript-value") "always-rewrite"))
      (reset-prefs-to-defaults!))
    (testing "body pre-handler"
      (set-pref! :body-pre-handler (fn [value] (if (= value ["cljs-value"]) ["handled-cljs-value"])))
      (is (= (body-api-call ["cljs-value"]) nil))
      (is (= (body-api-call ["non-matching-cljs-value"]) nil))
      (reset-prefs-to-defaults!))
    (testing "header post-handler"
      (set-pref! :body-post-handler (fn [_value] "always-rewrite"))
      (is (= (body-api-call ["cljs-value"]) "always-rewrite"))
      (is (= (body-api-call "javascript-value") "always-rewrite"))
      (reset-prefs-to-defaults!))))

(deftest test-meta
  (testing "meta is disabled"
    (set-pref! :print-meta-data false)
    (is-header (with-meta {} :meta)
      [::tag/cljs-land
       [::tag/header
        "{" "}"]])
    (reset-prefs-to-defaults!))
  (testing "simple meta"
    (is-header (with-meta {} :meta)
      [::tag/cljs-land
       [::tag/header
        [::tag/meta-wrapper
         "{" "}" [::tag/meta-reference REF]]]]
      (fn [ref]
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/meta-header "meta"]]])
        (is-body ref
          [::tag/meta-body
           [::tag/header
            [::tag/keyword ":meta"]]])))))

(deftest test-sequables
  (testing "min-sequable-count-for-expansion"
    (with-prefs {:max-header-elements           100
                 :min-expandable-sequable-count 3}
      (is-header [1 2]
        [::tag/cljs-land
         [::tag/header
          "["
          [::tag/integer 1]
          :spacer
          [::tag/integer 2]
          "]"]])
      (is-header [1 2 3]
        [::tag/cljs-land
         [::tag/header
          REF]]
        (fn [ref]
          (is (surrogate? ref))
          (has-body? ref true)
          (is-header ref
            [::tag/expandable
             [::tag/expandable-inner
              "["
              [::tag/integer 1]
              :spacer
              [::tag/integer 2]
              :spacer
              [::tag/integer 3]
              "]"]]))))
    (with-prefs {:max-header-elements           100
                 :min-expandable-sequable-count 4}
      (is-header [1 2 3]
        [::tag/cljs-land
         [::tag/header
          "["
          [::tag/integer 1]
          :spacer
          [::tag/integer 2]
          :spacer
          [::tag/integer 3]
          "]"]]))
    (with-prefs {:min-expandable-sequable-count nil}
      (is-header []
        [::tag/cljs-land
         [::tag/header
          REF]])
      (is-header [1 2 3]
        [::tag/cljs-land
         [::tag/header
          REF]])
      (is-header [1 2 3 4 5 6]
        [::tag/cljs-land
         [::tag/header
          REF]]
        (fn [ref]
          (is (surrogate? ref))
          (is-header ref
            [::tag/expandable
             [::tag/expandable-inner
              "["
              (unroll (fn [i] [[::tag/integer (+ i 1)] :spacer]) (range 5))
              :more-marker
              "]"]])
          (has-body? ref true)
          (is-body ref
            [::tag/body
             [::tag/standard-ol
              (unroll (fn [i] [[::tag/standard-li
                                [::tag/index i :line-index-separator]
                                [::tag/item
                                 [::tag/integer (+ i 1)]]]]) (range 6))]]))))))

(deftest test-circular-data
  (testing "circular data structure"
    (let [circular-ds (volatile! nil)]
      (vreset! circular-ds circular-ds)
      (is-header circular-ds
        [::tag/cljs-land
         [::tag/header
          [::tag/instance-header
           [::tag/instance-value REF]
           [::tag/instance-custom-printing-wrapper
            :instance-custom-printing-background
            [::tag/instance-custom-printing
             "#object [cljs.core.Volatile "
             "{"
             [::tag/keyword ":val"]
             :spacer
             REF
             "}"
             "]"]]
           [::tag/type-wrapper
            :type-header-background
            [::tag/type-ref REF]]]]]
        (fn [ref]
          (is-header ref
            [::tag/expandable
             [::tag/expandable-inner
              [::tag/fields-header
               :more-fields-symbol]]]))
        (fn [ref]
          (is-header ref
            [::tag/expandable
             [::tag/expandable-inner
              [::tag/circular-reference
               :circular-ref-icon]]])
          (is-body ref
            [::tag/circular-reference-body
             [::tag/instance-header
              [::tag/instance-value REF]
              [::tag/instance-custom-printing-wrapper
               :instance-custom-printing-background
               [::tag/instance-custom-printing
                "#object [cljs.core.Volatile "
                "{"
                [::tag/keyword ":val"]
                :spacer
                REF
                "}"
                "]"]]
              [::tag/type-wrapper
               :type-header-background
               [::tag/type-ref REF]]]]))
        (fn [ref])))))

(deftest test-function-formatting
  (testing "cljs-function?"
    (testing "these should NOT be recognized as cljs functions"
      (are [f] (not (cljs-function? f))
        b/simplest-fn))
    (testing "these should be recognized as cljs functions"
      (are [f] (cljs-function? f)
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
      (are [f] (not (cljs-function? f))
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
      (reset-prefs-to-defaults!)))
  (testing "minimal function formatting"
    (is-header b/minimal-fn
      [::tag/cljs-land
       [::tag/header REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/fn-header
             [::tag/fn-prefix :lambda-icon]
             [::tag/fn-args (pref-str :args-open-symbol :args-close-symbol)]]]])
        (is-body ref
          [::tag/body
           [::tag/standard-ol-no-margin
            [::tag/aligned-li :native-icon NATIVE-REF]]]))))

  (testing "cljs-lambda-multi-arity function formatting"
    (is-header b/cljs-lambda-multi-arity
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/fn-header
             [::tag/fn-prefix
              :lambda-icon]
             [::tag/fn-args (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]]]])
        (is-body ref
          [::tag/body
           [::tag/standard-ol-no-margin
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :lambda-icon]]
             [::tag/fn-args (pref-str :args-open-symbol :args-close-symbol)]]
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :lambda-icon]]
             [::tag/fn-args (pref-str :args-open-symbol "a b" :args-close-symbol)]]
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :lambda-icon]]
             [::tag/fn-args (pref-str :args-open-symbol "c d e f" :args-close-symbol)]]
            [::tag/aligned-li :native-icon NATIVE-REF]]]))))
  (testing "cljs-fn-multi-arity-var function formatting"
    (is-header b/cljs-fn-multi-arity-var
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/fn-header
             [::tag/fn-prefix :fn-icon [::tag/fn-name "cljs-fn-multi-arity-var"]]
             [::tag/fn-args (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]]]])
        (is-body ref
          [::tag/body
           [::tag/standard-ol-no-margin
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :fn-icon [::tag/fn-name "cljs-fn-multi-arity-var"]]]
             [::tag/fn-args (pref-str :args-open-symbol "a1" :args-close-symbol)]]
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :fn-icon [::tag/fn-name "cljs-fn-multi-arity-var"]]]
             [::tag/fn-args (pref-str :args-open-symbol "a2-1 a2-2" :args-close-symbol)]]
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :fn-icon [::tag/fn-name "cljs-fn-multi-arity-var"]]]
             [::tag/fn-args (pref-str :args-open-symbol "a3-1 a3-2 a3-3 a3-4" :args-close-symbol)]]
            [::tag/aligned-li
             [::tag/fn-multi-arity-args-indent
              [::tag/fn-prefix :fn-icon
               [::tag/fn-name "cljs-fn-multi-arity-var"]]]
             [::tag/fn-args (pref-str :args-open-symbol "va1 va2 & rest" :args-close-symbol)]]
            [::tag/aligned-li :ns-icon [::tag/fn-ns-name "devtools.utils.batteries"]]
            [::tag/aligned-li :native-icon NATIVE-REF]]])))))

(deftest test-alt-printer-impl
  (testing "wrapping IPrintWithWriter products as references if needed (issue #21)"                                           ; https://github.com/binaryage/cljs-devtools/issues/21
    (let [date-map {:date (goog.date.Date. 2016 6 1)}]                                                                        ; see extend-protocol IPrintWithWriter for goog.date.Date in batteries
      (is-header date-map
        [::tag/cljs-land
         [::tag/header
          "{"
          [::tag/keyword ":date"]
          :spacer
          "#gdate "
          42
          REF
          REF
          REF
          REF
          REF
          REF
          "}"]]
        (fn [ref]
          (is-header ref
            [::tag/header
             "["
             [::tag/integer 2016]
             :spacer
             [::tag/integer 6]
             :spacer
             [::tag/integer 1]
             "]"]))
        (fn [ref]
          (is-header ref
            [::tag/header
             "#js ["
             [::tag/string "\"test-array\""]
             "]"]))
        (fn [ref]
          (is-header ref
            [::tag/header
             "#js "
             "{"
             [::tag/keyword ":some-key"]
             :spacer
             [::tag/string "\"test-js-obj\""]
             "}"]))
        (fn [ref]
          (is-header ref
            [::tag/header
             [::tag/keyword ":keyword"]]))
        (fn [ref]
          (is-header ref
            [::tag/header
             [::tag/symbol "sym"]]))
        (fn [ref]
          (is-header ref
            [::tag/header
             "#\"" "regex" "\""]))))))
