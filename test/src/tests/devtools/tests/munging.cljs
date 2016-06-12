(ns devtools.tests.munging
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [clojure.string :as string]
            [devtools.util :refer-macros [oget oset ocall]]
            [devtools.munging :as m]
            [devtools.core :as devtools]
            [devtools.utils.batteries :as b]
            [devtools.utils.test :refer [match? match-seqs?]]))

(deftest test-cljs-fn-name
  (testing "these names should be detected as cljs functions"
    (are [name] (= true (m/cljs-fn-name? name))
      "cljs$core$first"
      "cljs$core$"
      "devtools_sample$core$hello"
      "devtools$sample$core$hello$a$bb$ccc$dddd$"))
  (testing "these should NOT be detected as cljs functions"
    (are [name] (not= true (m/cljs-fn-name? name))
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
    (are [code info] (= (m/parse-fn-source code) info)
      b/simplest-fn-source ["" ""]
      b/simple-lambda-fn-source ["" "p1, p2, p3"]
      b/simple-cljs-fn-source ["devtools_sample$core$hello" "name"]
      b/invalid-fn-source nil
      "" nil
      "???" nil)))

(deftest test-trivial-fn-source
  (testing "some functions are considered trivial and belong to js lands"
    (are [source] (m/trivial-fn-source? source)
      "function () {}"
      b/simplest-fn-source
      "function Function() { [native code] }"))
  (testing "all other functions are considered non-trivial"
    (are [source] (not (m/trivial-fn-source? source))
      b/simple-lambda-fn-source
      b/simple-cljs-fn-source
      b/invalid-fn-source
      "xxx"
      ""
      "\n")))

(deftest test-get-fn-source-safely
  (testing "exercise get-fn-source-safely"
    (are [f re] (if (string? re)
                  (= re (m/get-fn-source-safely f))
                  (some? (re-matches re (string/replace (m/get-fn-source-safely f) "\n" " "))))
      b/sample-cljs-fn #"function devtools\$utils\$batteries\$sample_cljs_fn\(var_args\) \{.*\}"
      b/simplest-fn "function () {}"
      nil ""
      1 ""
      "xxx" "")))

(deftest test-cljs-fn
  (testing "these things should be recognized as cljs functions"
    (are [f] (m/cljs-fn? f)
      b/sample-cljs-fn
      (oget b/sample-cljs-fn "prototype" "constructor")
      devtools/set-pref!
      devtools/available?
      devtools/is-feature-installed?
      devtools/uninstall!
      b/minimal-fn
      b/inst-type-ifn0
      b/inst-type-ifn1
      b/inst-type-ifn2
      b/inst-type-ifn2va
      b/inst-type-ifn4va))
  (testing "these things should NOT be recognized as cljs functions"
    (are [f] (not (m/cljs-fn? f))
      js/alert
      js/console
      b/simplest-fn
      b/test-reify
      b/test-lang
      (js/Function.)
      b/inst-some-type
      (oget js/window "document")
      (oget js/window "document" "getElementById")
      (oget b/sample-cljs-fn "__proto__"))))

(deftest test-dollar-preserving-demunge
  (testing "exercise dollar preserving demunging"
    (are [munged demunged] (= (m/dollar-preserving-demunge munged) demunged)
      "" ""
      "hello" "hello"
      "hello$world" "hello$world")))

(deftest test-parse-fn-info
  (testing "exercise parsing fn infos"
    (are [f expected] (match-seqs? (m/parse-fn-info f) expected)
      b/sample-cljs-fn ["devtools.utils.batteries" "sample-cljs-fn" "var-args"]
      b/cljs-fn-with-vec-destructuring ["devtools.utils.batteries" "cljs-fn-with-vec-destructuring" #"p--\d+"]
      b/cljs-fn-with-vec-destructuring-var ["devtools.utils.batteries" "cljs-fn-with-vec-destructuring-var" "var-args"]
      b/cljs-fn-with-map-destructuring ["devtools.utils.batteries" "cljs-fn-with-map-destructuring" #"p--\d+"]
      b/cljs-fn-with-map-destructuring-var ["devtools.utils.batteries" "cljs-fn-with-map-destructuring-var" "var-args"])))

(deftest test-human-readable-names
  (testing "exercise char-to-subscript"
    (testing "valid inputs to char-to-subscript"
      (are [input output] (= (m/char-to-subscript input) output)
        "1" "₁"
        "2" "₂"
        "3" "₃"
        "4" "₄"
        "5" "₅"
        "6" "₆"
        "7" "₇"
        "8" "₈"
        "9" "₉"
        "0" "₀"))
    (testing "invalid inputs to char-to-subscript"
      (are [input] (thrown? :default (m/char-to-subscript input))
        ""
        nil
        "xxx"
        :xxx)))
  (testing "exercise make-subscript"
    (testing "valid inputs to make-subscript"
      (are [input output] (= (m/make-subscript input) output)
        1234567890 "₁₂₃₄₅₆₇₈₉₀"
        111 "₁₁₁"
        3 "₃"
        0 "₀"))
    (testing "invalid inputs to make-subscript"
      (are [input] (thrown? :default (m/make-subscript input))
        "123"
        ""
        nil
        "xxx"
        :xxx)))
  (testing "exercise find-index-of-human-prefix"
    (are [input index] (= (m/find-index-of-human-prefix input) index)
      "abc" nil
      "123" nil
      "a--2" 1
      "abc--" 3
      "--x" nil
      "" nil
      "se12" 2
      "aaa--bbbd22" 3
      "aaac--bbb22" 4
      "PPP12XXXxx--" 3
      "PPPX12XXX--" 4
      "xxxyyyxxxyyy78aaabbbcccddd20xxx--zzz" 12))
  (testing "exercise humanize-names"
    (are [input output] (= (m/humanize-names input) output)
      ["p1" "p2" "p3"] ["p1" "p2" "p3"]
      ["p" "p" "p"] ["p" "p₂" "p₃"]
      ["p12" "p13" "p15"] ["p" "p₂" "p₃"]
      ["p--a" "p--b" "p--c"] ["p" "p₂" "p₃"]
      ["p--a" "p" "p--c"] ["p" "p₂" "p₃"]
      ["a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a"] ["a" "a₂" "a₃" "a₄" "a₅" "a₆" "a₇" "a₈" "a₉" "a₁₀" "a₁₁" "a₁₂"]
      ["aa--x" "bb23" "aa89" "bb--z"] ["aa" "bb" "aa₂" "bb₂"])))

(deftest test-arities
  (testing "exercise collect-fn-arities"
    (are [f arities] (= (set (keys (m/collect-fn-arities f))) arities)
      b/minimal-fn #{}
      b/simplest-fn #{}
      b/clsj-fn-with-fancy-name#$%!? #{}
      b/sample-cljs-fn #{:devtools.munging/variadic}
      b/cljs-fn-with-vec-destructuring #{}
      b/cljs-fn-with-vec-destructuring-var #{:devtools.munging/variadic}
      b/cljs-fn-with-map-destructuring #{}
      b/cljs-fn-with-map-destructuring-var #{:devtools.munging/variadic}
      b/cljs-fn-var #{:devtools.munging/variadic}
      b/cljs-fn-multi-arity #{1 2 4}
      b/cljs-fn-multi-arity-var #{1 2 4 :devtools.munging/variadic}
      b/inst-type-ifn0 #{0}
      b/inst-type-ifn1 #{1}
      b/inst-type-ifn2 #{1 2}
      b/inst-type-ifn2va #{:devtools.munging/variadic}
      b/inst-type-ifn4va #{0 1 4 :devtools.munging/variadic})))

(deftest test-ui-strings
  (testing "exercise args-lists-to-strings"
    (are [f expected] (match-seqs? (m/extract-args-strings f true " " "..." " & ") expected)
      b/minimal-fn [""]
      b/simplest-fn [""]
      b/clsj-fn-with-fancy-name#$%!? ["arg1! arg? *&mo-re"]
      b/sample-cljs-fn ["p1 p2 & rest"]
      b/cljs-fn-with-vec-destructuring ["p"]
      b/cljs-fn-with-vec-destructuring-var ["& p"]
      b/cljs-fn-with-map-destructuring ["p"]
      b/cljs-fn-with-map-destructuring-var ["& p"]
      b/cljs-fn-var ["first second & rest"]
      b/cljs-fn-multi-arity ["a1" "a2-1 a2-2" "a3-1 a3 a3₂ a3-4"]
      b/cljs-fn-multi-arity-var ["a1" "a2-1 a2-2" "a3-1 a3-2 a3-3 a3-4" "va1 va2 & rest"]
      b/inst-type-ifn0 [""]
      b/inst-type-ifn1 ["p1"]
      b/inst-type-ifn2 ["p1" "p1 p2"]
      b/inst-type-ifn2va ["p1 p2 & rest"]
      b/inst-type-ifn4va ["" "p1" "p p₂ p₃ p₄" "p p₂ p₃ p₄ & p₅"])))
