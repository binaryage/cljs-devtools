(ns devtools.tests.toolbox
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.tests.utils.test :refer [js-equals is-header is-body has-body? unroll remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.toolbox :as t]
            [devtools.tests.env.core :as env :refer [REF]]))

; -- envelope ---------------------------------------------------------------------------------------------------------------

(def default-envelope (t/envelope "default-envelope"))
(def custom-header-envelope (t/envelope "custom-header-envelope" "custom header"))
(def fn-header-envelope (t/envelope "fn-header-envelope" #(str "HEADER: " %)))
(def fully-custom-envelope (t/envelope "fully-custom-envelope" "header" "color:purple" "div"))

(deftest test-envelope
  (testing "default-envelope rendering"
    (is-header default-envelope
      [:default-envelope-tag :default-envelope-header])
    (has-body? default-envelope true)
    (is-body default-envelope
      [:body-tag
       [:standard-ol-tag
        [:standard-li-tag
         REF]]]
      (fn [ref]
        (is-header ref
          [:cljs-land-tag
           [:header-tag
            [:string-tag
             "\"default-envelope\""]]]))))
  (testing "custom-header-envelope rendering"
    (is-header custom-header-envelope
      [:default-envelope-tag "custom header"])
    (has-body? custom-header-envelope true)
    (is-body custom-header-envelope
      [:body-tag
       [:standard-ol-tag
        [:standard-li-tag
         REF]]]
      (fn [ref]
        (is-header ref
          [:cljs-land-tag
           [:header-tag
            [:string-tag
             "\"custom-header-envelope\""]]]))))
  (testing "fn-header-envelope rendering"
    (is-header fn-header-envelope
      [:default-envelope-tag "HEADER: fn-header-envelope"])
    (has-body? fn-header-envelope true)
    (is-body fn-header-envelope
      [:body-tag
       [:standard-ol-tag
        [:standard-li-tag
         REF]]]
      (fn [ref]
        (is-header ref
          [:cljs-land-tag
           [:header-tag
            [:string-tag
             "\"fn-header-envelope\""]]]))))
  (testing "fully-custom-envelope"
    (is-header fully-custom-envelope
      ["div" {"style" "color:purple"} "header"])
    (has-body? fully-custom-envelope true)
    (is-body fully-custom-envelope
      [:body-tag
       [:standard-ol-tag
        [:standard-li-tag
         REF]]]
      (fn [ref]
        (is-header ref
          [:cljs-land-tag
           [:header-tag
            [:string-tag
             "\"fully-custom-envelope\""]]])))))

; -- force-format -----------------------------------------------------------------------------------------------------------

(def string-val "string")
(def ff-string (t/force-format string-val))
(def integer-val 100)
(def ff-integer (t/force-format integer-val))
(def float-val 1.1)
(def ff-float (t/force-format float-val))
(def nil-val nil)
(def ff-nil (t/force-format nil-val))
(def regexp-val #"regexp")
(def ff-regexp (t/force-format regexp-val))

(deftest test-force-format
  (testing "force-format rendering for string"
    (is-header ff-string
      [:header-tag
       [:string-tag
        (str "\"" string-val "\"")]])
    (has-body? ff-string false))
  (testing "force-format rendering for integer"
    (is-header ff-integer
      [:header-tag
       [:integer-tag
        integer-val]])
    (has-body? ff-string false))
  (testing "force-format rendering for float"
    (is-header ff-float
      [:header-tag
       [:float-tag
        float-val]])
    (has-body? ff-float false))
  (testing "force-format rendering for nil"
    (is-header ff-nil
      [:header-tag
       [:nil-tag
        "nil"]])
    (has-body? ff-nil false))
  (testing "force-format rendering for regexp"
    (is-header ff-regexp
      [:header-tag "#\"" "regexp" "\""])
    (has-body? ff-regexp false)))
