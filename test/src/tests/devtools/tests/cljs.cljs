(ns devtools.tests.cljs
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.utils.test :refer [js-equals is-header is-body has-body? unroll]]
            [devtools.format :refer [surrogate?]]
            [devtools.prefs :refer [default-prefs merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]))

; test some fragile behaviours of CLJS printing we depend on
; mostly to catch changes in code like this:
; https://github.com/clojure/clojurescript/commit/34c3b8985ed8197d90f441c46d168c4024a20eb8

(deftype TemplateWriter [group]
  Object
  (merge [_ a] (.apply (.-push group) group a))
  (get-group [_] group)
  IWriter
  (-write [_ o] (.push group o))
  (-flush [_] nil))

(defn managed-pr-str [value]
  (let [writer (TemplateWriter. #js [])]
    (pr-seq-writer [value] writer {:print-length 10
                                   :print-level  5})
    (.get-group writer)))

(deftype SomeType [some-field])

(defn konstruktor [])

(deftest test-printing-edge-cases
  (testing "print function"
    (let [res (managed-pr-str #())]
      (is (= (count res) 5))
      (is (= (aget res 0) "#object["))
      (is (= (aget res 4) "\"]"))))
  (testing "print type (:else -cljs$lang$ctorStr case)"
    (let [res (managed-pr-str (SomeType. "some-value"))]
      (is (= (count res) 3))
      (is (= (aget res 0) "#object["))
      (is (= (aget res 2) "]"))))
  (testing "print type (:else -constructor case)"
    (let [res (managed-pr-str (konstruktor.))]
      (is (= (count res) 5))
      (is (= (aget res 0) "#object["))
      (is (= (aget res 4) "]")))))
