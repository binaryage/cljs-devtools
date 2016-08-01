(ns devtools.tests.format
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.tests.utils.macros :refer [range = > < + str want? with-prefs]])                                 ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.tests.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                               remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.formatters.helpers :refer [cljs-function? instance-of-a-well-known-type?]]
            [devtools.prefs :refer [pref]]
            [devtools.tests.env.core :as env :refer [REF NATIVE-REF SOMETHING]]))

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
      (env/SimpleType. "some-value")
      (range :max-number-body-items)
      (goog.date.Date.)                                                                                                       ; see extend-protocol IPrintWithWriter for goog.date.Date in env
      (goog.date.DateTime.)                                                                                                   ; inherits from goog.date.Date
      ; TODO!
      ;(goog.Promise.)                                                                                                         ; see extend-protocol IFormat for goog.Promise in env
      (env/get-raw-js-obj-implementing-iformat)
      (env/get-raw-js-obj-implementing-iprintwithwriter))))

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
      (env/SimpleType. "some-value")
      (range :max-number-body-items))))

(deftest test-simple-atomic-values
  (testing "keywords"
    (is-header :keyword
      [:cljs-land-tag
       [:header-tag
        [:keyword-tag ":keyword"]]])
    (is-header ::auto-namespaced-keyword
      [:cljs-land-tag
       [:header-tag
        [:keyword-tag ":devtools.tests.format/auto-namespaced-keyword"]]])
    (is-header :devtools/fully-qualified-keyword
      [:cljs-land-tag
       [:header-tag
        [:keyword-tag ":devtools/fully-qualified-keyword"]]]))
  (testing "symbols"
    (is-header 'symbol
      [:cljs-land-tag
       [:header-tag
        [:symbol-tag "symbol"]]])))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      [:cljs-land-tag
       [:header-tag
        [:string-tag (str :dq "some short string" :dq)]]])
    (is-header "line1\nline2\n\nline4"
      [:cljs-land-tag
       [:header-tag
        [:string-tag (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:string-tag (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)]]])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:string-tag
             (str
               :dq
               "1234" :new-line-string-replacer "678901234567890"
               :string-abbreviation-marker
               "12345678901234" :new-line-string-replacer "67890"
               :dq)]]])
        (is-body ref
          [:expanded-string-tag
           (str
             "1234" :new-line-string-replacer
             "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
             "\n67890")])))))

(deftest test-collections
  (testing "vectors"
    (is (< 4 :max-header-elements))
    (is (> 6 :min-expandable-sequable-count-for-well-known-types))
    (is-header [1 2 3]
      [:cljs-land-tag
       [:header-tag
        "["
        [:integer-tag 1] :spacer
        [:integer-tag 2] :spacer
        [:integer-tag 3]
        "]"]])
    (is-header [1 2 3 4 5]
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            "["
            (unroll (fn [i] [[:integer-tag (+ i 1)] :spacer]) (range 4))
            [:integer-tag 5]
            "]"]]))))
  (testing "ranges"
    (is (> 10 :max-header-elements))
    (is-header (range 10)
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            "("
            (unroll (fn [i] [[:integer-tag i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])))))

(deftest test-continuations
  (testing "long range"
    (is-header (range (+ :max-number-body-items 1))
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            "("
            (unroll (fn [i] [[:integer-tag i] :spacer]) (range :max-header-elements))
            :more-marker
            ")"]])
        (is-body ref
          [:body-tag
           [:standard-ol-tag
            (unroll (fn [i] [[:standard-li-tag
                              [:index-tag i :line-index-separator]
                              [:item-tag
                               [:integer-tag i]]]]) (range :max-number-body-items))
            [:standard-li-tag
             REF]]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                [:body-items-more-tag
                 :body-items-more-label]]])
            (is-body ref
              [:standard-ol-no-margin-tag
               (unroll (fn [i] [[:standard-li-no-margin-tag
                                 [:index-tag :max-number-body-items :line-index-separator]
                                 [:item-tag
                                  [:integer-tag (+ i :max-number-body-items)]]]]) (range 1))])))))))

(deftest test-printing
  (testing "max print level"
    (let [many-levels [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]]
      (has-body? many-levels false)
      (is-header many-levels
        [:cljs-land-tag
         [:header-tag
          "[" [:integer-tag 1] " "
          "[" [:integer-tag 2] " "
          REF
          "]"
          "]"]]
        (fn [ref]
          (is (surrogate? ref))
          (has-body? ref true)
          (is-header ref
            [:expandable-tag
             [:expandable-inner-tag
              "[" :more-marker "]"]]))))))

(deftest test-handlers
  (let [handled-output (clj->js (remove-empty-styles ["span" {"style" (pref :cljs-land-style)}
                                                      ["span" {"style" (pref :header-style)}
                                                       ["span" {"style" (pref :keyword-style)} ":handled"]]]))]
    (testing "header pre-handler"
      (with-prefs {:header-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled))}
        (is (js-equals (header-api-call ["cljs-value"]) handled-output))
        (is (not (js-equals (header-api-call ["non-matching-cljs-value"]) handled-output))))
      (with-prefs {:header-pre-handler (fn [value] (if (= value "javascript-value") :handled))}
        (is (js-equals (header-api-call "javascript-value") handled-output))
        (is (not (js-equals (header-api-call "not-matching-javascript-value") handled-output)))))
    (testing "header post-handler"
      (with-prefs {:header-post-handler (fn [_value] "always-rewrite")}
        (is (= (header-api-call ["cljs-value"]) "always-rewrite"))
        (is (= (header-api-call "javascript-value") "always-rewrite"))))
    (testing "has-body pre-handler"
      (with-prefs {:has-body-pre-handler (fn [value] (if (= value ["cljs-value"]) :handled))}
        (is (= (has-body-api-call ["cljs-value"]) false))
        (is (= (has-body-api-call :non-matching-cljs-value) nil)))
      (with-prefs {:has-body-pre-handler (fn [value] (if (= value "javascript-value") :handled))}
        (is (= (has-body-api-call "javascript-value") false))
        (is (= (has-body-api-call "not-matching-javascript-value") nil))))
    (testing "has-body post-handler"
      (with-prefs {:has-body-post-handler (fn [_value] "always-rewrite")}
        (is (= (has-body-api-call ["cljs-value"]) "always-rewrite"))
        (is (= (has-body-api-call "javascript-value") "always-rewrite"))))
    (testing "body pre-handler"
      (with-prefs {:body-pre-handler (fn [value] (if (= value ["cljs-value"]) ["handled-cljs-value"]))}
        (is (= (body-api-call ["cljs-value"]) nil))
        (is (= (body-api-call ["non-matching-cljs-value"]) nil))))
    (testing "header post-handler"
      (with-prefs {:body-post-handler (fn [_value] "always-rewrite")}
        (is (= (body-api-call ["cljs-value"]) "always-rewrite"))
        (is (= (body-api-call "javascript-value") "always-rewrite"))))))

(deftest test-meta
  (testing "meta is disabled"
    (with-prefs {:render-metas false}
      (is-header (with-meta {} :meta)
        [:cljs-land-tag
         [:header-tag
          "{" "}"]])))
  (testing "simple meta"
    (is-header (with-meta {} :meta)
      [:cljs-land-tag
       [:header-tag
        [:meta-wrapper-tag
         "{" "}" [:meta-reference-tag REF]]]]
      (fn [ref]
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:meta-header-tag "meta"]]])
        (is-body ref
          [:meta-body-tag
           [:header-tag
            [:keyword-tag ":meta"]]])))))

(deftest test-sequables
  (testing "min-sequable-count-for-expansion of a well known type"
    (let [v2 [1 2]
          v3 [1 2 3]
          v6 [1 2 3 4 5 6]]
      (are [v] (instance-of-a-well-known-type? v) v2 v3 v6)
      (with-prefs {:max-header-elements                                100
                   :min-expandable-sequable-count-for-well-known-types 3}
        (is-header v2
          [:cljs-land-tag
           [:header-tag
            "["
            [:integer-tag 1]
            :spacer
            [:integer-tag 2]
            "]"]])
        (is-header v3
          [:cljs-land-tag
           [:header-tag
            REF]]
          (fn [ref]
            (is (surrogate? ref))
            (has-body? ref true)
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                "["
                [:integer-tag 1]
                :spacer
                [:integer-tag 2]
                :spacer
                [:integer-tag 3]
                "]"]]))))
      (with-prefs {:max-header-elements                                100
                   :min-expandable-sequable-count-for-well-known-types 4}
        (is-header v3
          [:cljs-land-tag
           [:header-tag
            "["
            [:integer-tag 1]
            :spacer
            [:integer-tag 2]
            :spacer
            [:integer-tag 3]
            "]"]]))
      (with-prefs {:min-expandable-sequable-count-for-well-known-types 0}
        (is-header []
          [:cljs-land-tag
           [:header-tag
            "["
            "]"]])
        (is-header v3
          [:cljs-land-tag
           [:header-tag
            REF]])
        (is-header v6
          [:cljs-land-tag
           [:header-tag
            REF]]
          (fn [ref]
            (is (surrogate? ref))
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                "["
                (unroll (fn [i] [[:integer-tag (+ i 1)] :spacer]) (range 5))
                :more-marker
                "]"]])
            (has-body? ref true)
            (is-body ref
              [:body-tag
               [:standard-ol-tag
                (unroll (fn [i] [[:standard-li-tag
                                  [:index-tag i :line-index-separator]
                                  [:item-tag
                                   [:integer-tag (+ i 1)]]]]) (range 6))]]))))))
  (testing "min-sequable-count-for-expansion of a general type"
    (let [r0 (env/R0.)
          r1 (env/R1. 1)
          r2 (env/R2. 1 2)
          r3 (env/R3. 1 2 3)
          r6 (env/R6. 1 2 3 4 5 6)]
      (are [v] (not (instance-of-a-well-known-type? v)) r0 r1 r2 r3 r6)
      (with-prefs {:min-expandable-sequable-count 3}
        (is-header r2
          [:cljs-land-tag
           [:header-tag
            [:instance-header-tag
             :instance-header-background
             [:instance-value-tag REF]
             SOMETHING
             [:type-wrapper-tag
              :type-header-background
              [:type-ref-tag REF]]]]])
        (is-header r3
          [:cljs-land-tag
           [:header-tag
            REF]]
          (fn [ref]
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                [:instance-header-tag
                 :instance-header-background
                 [:instance-value-tag REF]
                 [:instance-custom-printing-wrapper-tag
                  :instance-custom-printing-background
                  SOMETHING]
                 [:type-wrapper-tag
                  :type-header-background
                  [:type-ref-tag REF]]]]]))))
      (with-prefs {:min-expandable-sequable-count 0}
        ; empty sequable should not expand regardless of :min-expandable-sequable-count
        (is-header r0
          [:cljs-land-tag
           [:header-tag
            [:instance-header-tag
             :instance-header-background
             [:instance-value-tag REF]
             SOMETHING
             [:type-wrapper-tag
              :type-header-background
              [:type-ref-tag REF]]]]])
        ; non-empty sequable should expand...
        (is-header r1
          [:cljs-land-tag
           [:header-tag
            REF]]
          (fn [ref]
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                [:instance-header-tag
                 :instance-header-background
                 [:instance-value-tag REF]
                 SOMETHING
                 [:type-wrapper-tag
                  :type-header-background
                  [:type-ref-tag REF]]]]])))))))

(deftest test-circular-data
  (testing "circular data structure"
    (let [circular-ds (volatile! nil)]
      (vreset! circular-ds circular-ds)
      (is-header circular-ds
        [:cljs-land-tag
         [:header-tag
          [:instance-header-tag
           :instance-header-background
           [:instance-value-tag REF]
           [:instance-custom-printing-wrapper-tag
            :instance-custom-printing-background
            [:instance-custom-printing-tag
             "#object [cljs.core.Volatile "
             "{"
             [:keyword-tag ":val"]
             :spacer
             REF
             "}"
             "]"]]
           [:type-wrapper-tag
            :type-header-background
            [:type-ref-tag REF]]]]]
        (fn [ref]
          (is-header ref
            [:expandable-tag
             [:expandable-inner-tag
              [:fields-header-tag
               :more-fields-symbol]]]))
        (fn [ref]
          (is-header ref
            [:expandable-tag
             [:expandable-inner-tag
              [:circular-reference-tag
               :circular-ref-icon]]])
          (is-body ref
            [:circular-reference-body-tag
             [:instance-header-tag
              :instance-header-background
              [:instance-value-tag REF]
              [:instance-custom-printing-wrapper-tag
               :instance-custom-printing-background
               [:instance-custom-printing-tag
                "#object [cljs.core.Volatile "
                "{"
                [:keyword-tag ":val"]
                :spacer
                REF
                "}"
                "]"]]
              [:type-wrapper-tag
               :type-header-background
               [:type-ref-tag REF]]]]))
        (fn [ref])))))

(deftest test-function-formatting
  (testing "cljs-function?"
    (testing "these should NOT be recognized as cljs functions"
      (are [f] (not (cljs-function? f))
        env/simplest-fn))
    (testing "these should be recognized as cljs functions"
      (are [f] (cljs-function? f)
        env/minimal-fn
        env/cljs-lambda-multi-arity
        env/clsj-fn-with-fancy-name#$%!?
        env/cljs-fn-var
        env/cljs-fn-multi-arity-var
        env/cljs-fn-multi-arity
        env/cljs-fn-with-vec-destructuring
        env/inst-type-ifn0
        env/inst-type-ifn1
        env/inst-type-ifn2
        env/inst-type-ifn2va
        env/inst-type-ifn4va))
    (testing "these should be recognized as cljs functions"
      (with-prefs {:disable-cljs-fn-formatting true}
        (are [f] (not (cljs-function? f))
          env/minimal-fn
          env/clsj-fn-with-fancy-name#$%!?
          env/cljs-fn-var
          env/cljs-fn-multi-arity-var
          env/cljs-fn-multi-arity
          env/cljs-fn-with-vec-destructuring
          env/inst-type-ifn0
          env/inst-type-ifn1
          env/inst-type-ifn2
          env/inst-type-ifn2va
          env/inst-type-ifn4va))))
  (testing "minimal function formatting"
    (is-header env/minimal-fn
      [:cljs-land-tag
       [:header-tag REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fn-header-tag
             [:fn-prefix-tag :lambda-icon]
             [:fn-args-tag (pref-str :args-open-symbol :args-close-symbol)]]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag :native-icon NATIVE-REF]]]))))

  (testing "cljs-lambda-multi-arity function formatting"
    (is-header env/cljs-lambda-multi-arity
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fn-header-tag
             [:fn-prefix-tag
              :lambda-icon]
             [:fn-args-tag (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :lambda-icon]]
             [:fn-args-tag (pref-str :args-open-symbol :args-close-symbol)]]
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :lambda-icon]]
             [:fn-args-tag (pref-str :args-open-symbol "a b" :args-close-symbol)]]
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :lambda-icon]]
             [:fn-args-tag (pref-str :args-open-symbol "c d e f" :args-close-symbol)]]
            [:aligned-li-tag :native-icon NATIVE-REF]]]))))
  (testing "cljs-fn-multi-arity-var function formatting"
    (is-header env/cljs-fn-multi-arity-var
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (has-body? ref true)
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fn-header-tag
             [:fn-prefix-tag :fn-icon [:fn-name-tag "cljs-fn-multi-arity-var"]]
             [:fn-args-tag (pref-str :args-open-symbol :multi-arity-symbol :args-close-symbol)]]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :fn-icon [:fn-name-tag "cljs-fn-multi-arity-var"]]]
             [:fn-args-tag (pref-str :args-open-symbol "a1" :args-close-symbol)]]
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :fn-icon [:fn-name-tag "cljs-fn-multi-arity-var"]]]
             [:fn-args-tag (pref-str :args-open-symbol "a2-1 a2-2" :args-close-symbol)]]
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :fn-icon [:fn-name-tag "cljs-fn-multi-arity-var"]]]
             [:fn-args-tag (pref-str :args-open-symbol "a3-1 a3-2 a3-3 a3-4" :args-close-symbol)]]
            [:aligned-li-tag
             [:fn-multi-arity-args-indent-tag
              [:fn-prefix-tag :fn-icon
               [:fn-name-tag "cljs-fn-multi-arity-var"]]]
             [:fn-args-tag (pref-str :args-open-symbol "va1 va2 & rest" :args-close-symbol)]]
            [:aligned-li-tag :ns-icon [:fn-ns-name-tag "devtools.tests.env.core"]]
            [:aligned-li-tag :native-icon NATIVE-REF]]])))))

(deftest test-alt-printer-impl
  (testing "wrapping IPrintWithWriter products as references if needed (issue #21)"                                           ; https://github.com/binaryage/cljs-devtools/issues/21
    (let [date-map {:date (goog.date.Date. 2016 6 1)}]                                                                        ; see extend-protocol IPrintWithWriter for goog.date.Date in batteries
      (is-header date-map
        [:cljs-land-tag
         [:header-tag
          "{"
          [:keyword-tag ":date"]
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
            [:header-tag
             "["
             [:integer-tag 2016]
             :spacer
             [:integer-tag 6]
             :spacer
             [:integer-tag 1]
             "]"]))
        (fn [ref]
          (is-header ref
            [:header-tag
             "#js ["
             [:string-tag "\"test-array\""]
             "]"]))
        (fn [ref]
          (is-header ref
            [:header-tag
             "#js "
             "{"
             [:keyword-tag ":some-key"]
             :spacer
             [:string-tag "\"test-js-obj\""]
             "}"]))
        (fn [ref]
          (is-header ref
            [:header-tag
             [:keyword-tag ":keyword"]]))
        (fn [ref]
          (is-header ref
            [:header-tag
             [:symbol-tag "sym"]]))
        (fn [ref]
          (is-header ref
            [:header-tag
             "#\"" "regex" "\""]))))))

(deftest test-types
  (testing "bare deftype with two fields"
    (is-header env/T2
      [:cljs-land-tag
       [:header-tag
        [:standalone-type-tag
         [:type-wrapper-tag
          :type-header-background
          [:type-ref-tag REF]]]]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:type-header-tag :type-symbol [:type-name-tag "T2"]]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag :basis-icon
             [:type-basis-tag
              [:type-basis-item-tag "fld1"]
              :type-basis-item-separator
              [:type-basis-item-tag "fld2"]]]
            [:aligned-li-tag :ns-icon
             [:fn-ns-name-tag "devtools.tests.env.core"]]
            [:aligned-li-tag :native-icon
             [:native-reference-wrapper-tag :native-reference-background
              [:native-reference-tag REF]]]]]))))
  (testing "bare deftype with no fields"
    (is-header env/T0
      [:cljs-land-tag
       [:header-tag
        [:standalone-type-tag
         [:type-wrapper-tag
          :type-header-background
          [:type-ref-tag REF]]]]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:type-header-tag :type-symbol [:type-name-tag "T0"]]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag :empty-basis-symbol]
            [:aligned-li-tag :ns-icon
             [:fn-ns-name-tag "devtools.tests.env.core"]]
            [:aligned-li-tag :native-icon
             [:native-reference-wrapper-tag :native-reference-background
              [:native-reference-tag REF]]]]]))))
  (testing "simple deftype with two fields (no protocols, no custom printer)"
    (is-header (env/T2. "val1" "val2")
      [:cljs-land-tag
       [:header-tag
        [:instance-header-tag
         :instance-header-background
         [:instance-value-tag REF]
         [:type-wrapper-tag
          :type-header-background
          [:type-ref-tag REF]]]]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fields-header-tag
             :fields-header-open-symbol
             [:header-field-tag
              [:header-field-name-tag "fld1"]
              :header-field-value-spacer
              [:header-field-value-tag REF]
              :header-field-separator]
             [:header-field-tag
              [:header-field-name-tag "fld2"]
              :header-field-value-spacer
              [:header-field-value-tag REF]
              :header-field-separator]
             :fields-header-close-symbol]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag :fields-icon
             [:instance-body-fields-table-tag
              [:body-field-tr-tag
               [:body-field-td1-tag :body-field-symbol [:body-field-name-tag "fld1"]]
               [:body-field-td2-tag :body-field-value-spacer]
               [:body-field-td3-tag [:body-field-value-tag REF]]]
              [:body-field-tr-tag
               [:body-field-td1-tag :body-field-symbol [:body-field-name-tag "fld2"]]
               [:body-field-td2-tag :body-field-value-spacer]
               [:body-field-td3-tag [:body-field-value-tag REF]]]]]
            ; note: no protocols here
            [:aligned-li-tag :native-icon
             [:native-reference-wrapper-tag :native-reference-background
              [:native-reference-tag REF]]]]]))
      (fn [_])))
  (testing "simple deftype with no fields (no protocols, no custom printer)"
    (is-header (env/T0.)
      [:cljs-land-tag
       [:header-tag
        [:instance-header-tag
         :instance-header-background
         [:instance-value-tag REF]
         [:type-wrapper-tag
          :type-header-background
          [:type-ref-tag REF]]]]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fields-header-tag
             :fields-header-no-fields-symbol]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            ; note: no fields table here
            ; note: no protocols here
            [:aligned-li-tag :native-icon
             [:native-reference-wrapper-tag :native-reference-background
              [:native-reference-tag REF]]]]]))
      (fn [_])))
  (testing "simple deftype with one field and custom printer"
    (is-header (env/T1+IPrintWithWriter. "val1")
      [:cljs-land-tag
       [:header-tag
        [:instance-header-tag
         :instance-header-background
         [:instance-value-tag REF]
         [:instance-custom-printing-wrapper-tag
          :instance-custom-printing-background
          [:instance-custom-printing-tag "from custom printer"]]
         [:type-wrapper-tag
          :type-header-background
          [:type-ref-tag REF]]]]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:fields-header-tag
             :fields-header-open-symbol
             :more-fields-symbol
             :fields-header-close-symbol]]])
        (is-body ref
          [:body-tag
           [:standard-ol-no-margin-tag
            [:aligned-li-tag :fields-icon
             [:instance-body-fields-table-tag
              [:body-field-tr-tag
               [:body-field-td1-tag :body-field-symbol [:body-field-name-tag "fld1"]]
               [:body-field-td2-tag :body-field-value-spacer]
               [:body-field-td3-tag [:body-field-value-tag REF]]]]]
            [:aligned-li-tag :protocols-icon
             [:protocols-header-tag
              [:fast-protocol-tag :protocol-background REF]]]
            [:aligned-li-tag :native-icon
             [:native-reference-wrapper-tag :native-reference-background
              [:native-reference-tag REF]]]]]))
      (fn [_])))
  (testing "known type instance"
    (let [example-known-type-instance (cljs.core/EmptyList. nil)]
      (is (instance-of-a-well-known-type? example-known-type-instance))
      (is-header example-known-type-instance
        [:cljs-land-tag                                                                                                       ; this is a proof that we didn't render via instance rendering path
         [:header-tag
          "()"]])))
  (testing "simple defrecord with one field (has implicit custom printer)"
    (is-header (env/R1. "val1")
      [:cljs-land-tag
       [:header-tag
        REF]]
      (fn [ref]
        (is-header ref
          [:expandable-tag
           [:expandable-inner-tag
            [:instance-header-tag
             :instance-header-background
             [:instance-value-tag REF]
             SOMETHING
             [:type-wrapper-tag
              :type-header-background
              [:type-ref-tag REF]]]]]
          (fn [ref]
            (is-header ref
              [:expandable-tag
               [:expandable-inner-tag
                [:fields-header-tag
                 :fields-header-open-symbol
                 :more-fields-symbol
                 :fields-header-close-symbol]]])
            (is-body ref
              [:body-tag
               [:standard-ol-no-margin-tag
                [:aligned-li-tag :fields-icon
                 [:instance-body-fields-table-tag
                  [:body-field-tr-tag
                   [:body-field-td1-tag :body-field-symbol [:body-field-name-tag "fld1"]]
                   [:body-field-td2-tag :body-field-value-spacer]
                   [:body-field-td3-tag [:body-field-value-tag REF]]]]]
                [:aligned-li-tag :protocols-icon REF]                                                                         ; expandable protocols
                [:aligned-li-tag :native-icon
                 [:native-reference-wrapper-tag :native-reference-background
                  [:native-reference-tag REF]]]]]
              (fn [_fld1-val-ref])
              (fn [protocols-ref]
                (is-header protocols-ref
                  [:expandable-tag
                   [:expandable-inner-tag
                    [:protocols-header-tag
                     :protocols-list-open-symbol
                     [:fast-protocol-tag :protocol-background REF]
                     [:fast-protocol-tag :protocol-background REF]
                     [:fast-protocol-tag :protocol-background REF]
                     [:fast-protocol-tag :protocol-background REF]
                     [:fast-protocol-tag :protocol-background REF]
                     [:slow-protocol-tag :protocol-background [:protocol-more-tag "+9…"]]
                     :protocols-list-close-symbol]]]
                  (fn [p1-ref]
                    (is-header p1-ref
                      [:expandable-tag
                       [:expandable-inner-tag
                        [:protocol-name-tag "IAssociative"]]]))
                  (fn [_p2-ref])
                  (fn [_p3-ref])
                  (fn [_p4-ref])
                  (fn [_p5-ref])))
              (fn [_native-ref])))
          (fn [_])))))
  ; TODO: test various protocol scenarios
  )
