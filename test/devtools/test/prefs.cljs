(ns devtools.test.prefs
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header want? is-body has-body? unroll]]
            [devtools.format :refer [surrogate?]]
            [devtools.prefs :refer [default-prefs set-prefs! get-prefs pref]]))

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
      (is (= (pref :span) "span")))))

(deftest test-render-with-changed-styles
  (testing "simple keyword styling"
    (is-header :keyword
      ["span" {"style" "background-color:#efe"}
       ["span" {"style" "color:#881391"} ":keyword"]])
    (set-prefs! (merge (get-prefs) {:cljs-style "background-color:red" :keyword-style "color:white"}))
    (is-header :keyword
      ["span" {"style" "background-color:red"}
       ["span" {"style" "color:white"} ":keyword"]])
    (set-prefs! default-prefs)))