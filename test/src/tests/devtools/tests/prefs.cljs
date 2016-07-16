(ns devtools.tests.prefs
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header is-body has-body? unroll reset-prefs-to-defaults!]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.defaults :as defaults]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]))

(deftest test-default-prefs
  (testing "default prefs should exist"
    (is (> (count defaults/prefs) 0)))
  (testing "some known default prefs values"
    (is (= (:span defaults/prefs) "span"))
    (is (= (:ol defaults/prefs) "ol"))
    (is (= (:li defaults/prefs) "li"))))

(deftest test-changing-prefs
  (testing "set prefs"
    (let [new-prefs (assoc (get-prefs) :span "div")]
      (set-prefs! new-prefs)
      (is (= (get-prefs) new-prefs))
      (is (= (pref :span) "div"))
      (reset-prefs-to-defaults!)
      (is (= (pref :span) "span"))))
  (testing "set pref"
    (set-pref! :span "div")
    (is (= (pref :span) "div"))
    (reset-prefs-to-defaults!))
  (testing "update pref"
    (let [max-print-level (pref :max-print-level)]
      (update-pref! :max-print-level inc)
      (is (= (pref :max-print-level) (inc max-print-level)))
      (reset-prefs-to-defaults!)))
  (testing "merge prefs"
    (merge-prefs! {:cljs-land-style "background-color:red" :keyword-style "color:white"})
    (is (= (pref :cljs-land-style) "background-color:red"))
    (is (= (pref :keyword-style) "color:white"))
    (reset-prefs-to-defaults!)))

(deftest test-render-with-changed-styles
  (testing "simple keyword styling"
    (is-header :keyword
      ["span" {"style" :cljs-land-style}
       ["span" {"style" :header-style}
        ["span" {"style" :keyword-style} ":keyword"]]])
    (merge-prefs! {:header-style "background-color:red" :keyword-style "color:white"})
    (is-header :keyword
      ["span" {"style" :cljs-land-style}
       ["span" {"style" "background-color:red"}
        ["span" {"style" "color:white"} ":keyword"]]])
    (reset-prefs-to-defaults!)))
