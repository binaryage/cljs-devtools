(ns devtools.tests.toolbox
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.util :refer-macros [oget oset ocall]]
            [devtools.utils.test :refer [js-equals is-header is-body has-body? unroll remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.pseudo.tag :as tag]
            [devtools.toolbox :as t]
            [devtools.utils.batteries :as b :refer [REF]]))

; -- envelope ---------------------------------------------------------------------------------------------------------------

(def default-envelope (t/envelope "default-envelope"))
(def custom-header-envelope (t/envelope "custom-header-envelope" "custom header"))
(def fn-header-envelope (t/envelope "fn-header-envelope" #(str "HEADER: " %)))
(def fully-custom-envelope (t/envelope "fully-custom-envelope" "header" "color:purple" "div"))

(deftest test-envelope
  (testing "default-envelope rendering"
    (is-header default-envelope
      [::tag/default-envelope :default-envelope-header])
    (has-body? default-envelope true)
    (is-body default-envelope
      [::tag/body
       [::tag/standard-ol
        [::tag/standard-li
         REF]]]
      (fn [ref]
        (is-header ref
          [::tag/cljs-land
           [::tag/header
            [::tag/string
             "\"default-envelope\""]]]))))
  (testing "custom-header-envelope rendering"
    (is-header custom-header-envelope
      [::tag/default-envelope "custom header"])
    (has-body? custom-header-envelope true)
    (is-body custom-header-envelope
      [::tag/body
       [::tag/standard-ol
        [::tag/standard-li
         REF]]]
      (fn [ref]
        (is-header ref
          [::tag/cljs-land
           [::tag/header
            [::tag/string
             "\"custom-header-envelope\""]]]))))
  (testing "fn-header-envelope rendering"
    (is-header fn-header-envelope
      [::tag/default-envelope "HEADER: fn-header-envelope"])
    (has-body? fn-header-envelope true)
    (is-body fn-header-envelope
      [::tag/body
       [::tag/standard-ol
        [::tag/standard-li
         REF]]]
      (fn [ref]
        (is-header ref
          [::tag/cljs-land
           [::tag/header
            [::tag/string
             "\"fn-header-envelope\""]]]))))
  (testing "fully-custom-envelope"
    (is-header fully-custom-envelope
      ["div" {"style" "color:purple"} "header"])
    (has-body? fully-custom-envelope true)
    (is-body fully-custom-envelope
      [::tag/body
       [::tag/standard-ol
        [::tag/standard-li
         REF]]]
      (fn [ref]
        (is-header ref
          [::tag/cljs-land
           [::tag/header
            [::tag/string
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
      [::tag/header
       [::tag/string
        (str "\"" string-val "\"")]])
    (has-body? ff-string false))
  (testing "force-format rendering for integer"
    (is-header ff-integer
      [::tag/header
       [::tag/integer
        integer-val]])
    (has-body? ff-string false))
  (testing "force-format rendering for float"
    (is-header ff-float
      [::tag/header
       [::tag/float
        float-val]])
    (has-body? ff-float false))
  (testing "force-format rendering for nil"
    (is-header ff-nil
      [::tag/header
       [::tag/nil
        "nil"]])
    (has-body? ff-nil false))
  (testing "force-format rendering for regexp"
    (is-header ff-regexp
      [::tag/header "#\"" "regexp" "\""])
    (has-body? ff-regexp false)))
