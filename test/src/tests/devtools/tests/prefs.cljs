(ns devtools.tests.prefs
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll]]
            [devtools.format :refer [surrogate?]]
            [devtools.prefs :refer [default-prefs merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]))

(deftest test-default-prefs
  (testing "default prefs should exist"
    (is (> (count default-prefs) 0)))
  (testing "some known default prefs values"
    (is (= (:span default-prefs) "span"))
    (is (= (:ol default-prefs) "ol"))
    (is (= (:li default-prefs) "li"))))

(deftest test-changing-prefs
  (testing "set prefs"
    (let [new-prefs (assoc (get-prefs) :span "div")]
      (set-prefs! new-prefs)
      (is (= (get-prefs) new-prefs))
      (is (= (pref :span) "div"))
      (set-prefs! default-prefs)
      (is (= (pref :span) "span"))))
  (testing "set pref"
    (set-pref! :span "div")
    (is (= (pref :span) "div"))
    (set-prefs! default-prefs))
  (testing "update pref"
    (let [max-print-level (pref :max-print-level)]
      (update-pref! :max-print-level inc)
      (is (= (pref :max-print-level) (inc max-print-level)))
      (set-prefs! default-prefs)))
  (testing "merge prefs"
    (merge-prefs! {:cljs-style "background-color:red" :keyword-style "color:white"})
    (is (= (pref :cljs-style) "background-color:red"))
    (is (= (pref :keyword-style) "color:white"))
    (set-prefs! default-prefs)))

(deftest test-render-with-changed-styles
  (testing "simple keyword styling"
    (is-header :keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" :header-style}
        ["span" {"style" :keyword-style} ":keyword"]]])
    (merge-prefs! {:header-style "background-color:red" :keyword-style "color:white"})
    (is-header :keyword
      ["span" {"style" :cljs-style}
       ["span" {"style" "background-color:red"}
        ["span" {"style" "color:white"} ":keyword"]]])
    (set-prefs! default-prefs)))