(ns devtools.tests.toolbox
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.util :refer-macros [oget oset ocall]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll remove-empty-styles pref-str]]
            [devtools.format :refer [surrogate? header-api-call has-body-api-call body-api-call]]
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
      [:default-envelope-tag {"style" :default-envelope-style} :default-envelope-header])
    (has-body? default-envelope true)
    (is-body default-envelope
      [:ol {"style" :standard-ol-style}
       [:li {"style" :standard-li-style}
        REF]]
      (fn [ref]
        (is-header ref
          [:span {"style" :cljs-style}
           [:span {"style" :header-style}
            [:span {"style" :string-style}
             "\"default-envelope\""]]]))))
  (testing "custom-header-envelope rendering"
    (is-header custom-header-envelope
      [:default-envelope-tag {"style" :default-envelope-style} "custom header"])
    (has-body? custom-header-envelope true)
    (is-body custom-header-envelope
      [:ol {"style" :standard-ol-style}
       [:li {"style" :standard-li-style}
        REF]]
      (fn [ref]
        (is-header ref
          [:span {"style" :cljs-style}
           [:span {"style" :header-style}
            [:span {"style" :string-style}
             "\"custom-header-envelope\""]]]))))
  (testing "fn-header-envelope rendering"
    (is-header fn-header-envelope
      [:default-envelope-tag {"style" :default-envelope-style} "HEADER: fn-header-envelope"])
    (has-body? fn-header-envelope true)
    (is-body fn-header-envelope
      [:ol {"style" :standard-ol-style}
       [:li {"style" :standard-li-style}
        REF]]
      (fn [ref]
        (is-header ref
          [:span {"style" :cljs-style}
           [:span {"style" :header-style}
            [:span {"style" :string-style}
             "\"fn-header-envelope\""]]]))))
  (testing "fully-custom-envelope"
    (is-header fully-custom-envelope
      ["div" {"style" "color:purple"} "header"])
    (has-body? fully-custom-envelope true)
    (is-body fully-custom-envelope
      [:ol {"style" :standard-ol-style}
       [:li {"style" :standard-li-style}
        REF]]
      (fn [ref]
        (is-header ref
          [:span {"style" :cljs-style}
           [:span {"style" :header-style}
            [:span {"style" :string-style}
             "\"fully-custom-envelope\""]]])))))

; -- force-format -----------------------------------------------------------------------------------------------------------

;(deftest test-force-format)
