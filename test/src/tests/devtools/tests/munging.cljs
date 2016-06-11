(ns devtools.tests.munging
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [clojure.string :as string]
            [devtools.util :refer-macros [oget oset ocall]]
            [devtools.munging :as munging]
            [devtools.core :as core]
            [devtools.format :as format]
            [devtools.utils.batteries :as batteries]))

(deftest test-cljs-fn-name
  (testing "these names should be detected as cljs functions"
    (are [name] (= true (munging/cljs-fn-name? name))
      "cljs$core$first"
      "cljs$core$"
      "devtools_sample$core$hello"
      "devtools$sample$core$hello$a$bb$ccc$dddd$"))
  (testing "these should NOT be detected as cljs functions"
    (are [name] (not= true (munging/cljs-fn-name? name))
      nil
      1
      ""
      "$"
      "$$"
      "$$$"
      "$$$$"
      "$$ $$"
      "hello"
      "hello$world")))

(deftest test-parse-fn-source
  (testing "parse some simple sources"
    (are [code info] (= (munging/parse-fn-source code) info)
      batteries/simplest-fn-source ["" ""]
      batteries/simple-lambda-fn-source ["" "p1, p2, p3"]
      batteries/simple-cljs-fn-source ["devtools_sample$core$hello" "name"]
      batteries/invalid-fn-source nil
      "" nil
      "???" nil)))

(deftest test-trivial-fn-source
  (testing "some functions are considered trivial and belong to js lands"
    (are [source] (munging/trivial-fn-source? source)
      "function () {}"
      batteries/simplest-fn-source
      "function Function() { [native code] }"))
  (testing "all other functions are considered non-trivial"
    (are [source] (not (munging/trivial-fn-source? source))
      batteries/simple-lambda-fn-source
      batteries/simple-cljs-fn-source
      batteries/invalid-fn-source
      "xxx"
      ""
      "\n")))

(deftest test-get-fn-source-safely
  (testing "exercise get-fn-source-safely"
    (are [f re] (if (string? re)
                  (= re (munging/get-fn-source-safely f))
                  (some? (re-matches re (string/replace (munging/get-fn-source-safely f) "\n" " "))))
      batteries/sample-cljs-fn #"function devtools\$utils\$batteries\$sample_cljs_fn\(var_args\) \{.*\}"
      batteries/simplest-fn "function () {}"
      nil ""
      1 ""
      "xxx" "")))

(deftest test-cljs-fn
  (testing "these things should be recognized as cljs functions"
    (are [f] (munging/cljs-fn? f)
      batteries/sample-cljs-fn
      (oget batteries/sample-cljs-fn "prototype" "constructor")
      core/set-pref!
      core/available?
      core/is-feature-installed?
      core/uninstall!
      batteries/minimal-fn
      batteries/inst-type-ifn0
      batteries/inst-type-ifn1
      batteries/inst-type-ifn2
      batteries/inst-type-ifn2va
      batteries/inst-type-ifn4va))
  (testing "these things should NOT be recognized as cljs functions"
    (are [f] (not (munging/cljs-fn? f))
      js/alert
      js/console
      batteries/simplest-fn
      batteries/test-reify
      batteries/test-lang
      (js/Function.)
      (batteries/SomeType. "xxx")
      (oget js/window "document")
      (oget js/window "document" "getElementById")
      (oget batteries/sample-cljs-fn "__proto__"))))

(deftest test-dollar-preserving-demunge
  (testing "exercise dollar preserving demunging"
    (are [munged demunged] (= (munging/dollar-preserving-demunge munged) demunged)
      "" ""
      "hello" "hello"
      "hello$world" "hello$world")))

(deftest test-parse-fn-info
  (testing "exercise parsing fn infos"
    (are [f info] (= (munging/parse-fn-info f) info)
      batteries/sample-cljs-fn ["devtools.utils.batteries" "sample-cljs-fn" "var-args"])))