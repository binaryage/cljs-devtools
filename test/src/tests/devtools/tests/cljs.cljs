(ns devtools.tests.cljs
  (:require-macros [devtools.oops :refer [unchecked-aget]])
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.tests.utils.test :refer [js-equals is-header is-body has-body? unroll]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [clojure.string :as string]))

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
      (is (= (unchecked-aget res 0) "#object["))
      (is (string/ends-with? (unchecked-aget res (dec (count res))) "]"))))
  (testing "print type (:else -cljs$lang$ctorStr case)"
    (let [res (managed-pr-str (SomeType. "some-value"))]
      (is (= (count res) 3))
      (is (= (unchecked-aget res 0) "#object["))
      (is (= (unchecked-aget res 2) "]"))))
  (testing "print type (:else -constructor case)"
    (let [res (managed-pr-str (konstruktor.))]
      (is (= (count res) 5))
      (is (= (unchecked-aget res 0) "#object["))
      (is (= (unchecked-aget res 4) "]")))))
