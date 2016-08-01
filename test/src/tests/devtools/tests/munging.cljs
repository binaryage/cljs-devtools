(ns devtools.tests.munging
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [clojure.string :as string]
            [devtools.util :refer-macros [oget oset ocall]]
            [devtools.munging :as m]
            [devtools.core :as devtools]
            [devtools.tests.env.core :as env]
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
      env/simplest-fn-source ["" ""]
      env/simple-lambda-fn-source ["" "p1, p2, p3"]
      env/simple-cljs-fn-source ["devtools_sample$core$hello" "name"]
      env/invalid-fn-source nil
      "" nil
      "???" nil)))

(deftest test-trivial-fn-source
  (testing "some functions are considered trivial and belong to js lands"
    (are [source] (m/trivial-fn-source? source)
      "function () {}"
      env/simplest-fn-source
      "function Function() { [native code] }"))
  (testing "all other functions are considered non-trivial"
    (are [source] (not (m/trivial-fn-source? source))
      env/simple-lambda-fn-source
      env/simple-cljs-fn-source
      env/invalid-fn-source
      "xxx"
      ""
      "\n")))

(deftest test-get-fn-source-safely
  (testing "exercise get-fn-source-safely"
    (are [f re] (if (string? re)
                  (= re (m/get-fn-source-safely f))
                  (some? (re-matches re (string/replace (m/get-fn-source-safely f) "\n" " "))))
      env/sample-cljs-fn #"function devtools\$tests\$env\$core\$sample_cljs_fn\(var_args\) \{.*\}"
      env/simplest-fn "function () {}"
      nil ""
      1 ""
      "xxx" "")))

(deftest test-cljs-fn
  (testing "these things should be recognized as cljs functions"
    (are [f] (m/cljs-fn? f)
      env/sample-cljs-fn
      (oget env/sample-cljs-fn "prototype" "constructor")
      devtools/set-pref!
      devtools/available?
      devtools/is-feature-installed?
      devtools/uninstall!
      env/minimal-fn
      env/inst-type-ifn0
      env/inst-type-ifn1
      env/inst-type-ifn2
      env/inst-type-ifn2va
      env/inst-type-ifn4va))
  (testing "these things should NOT be recognized as cljs functions"
    (are [f] (not (m/cljs-fn? f))
      js/alert
      js/console
      env/simplest-fn
      env/test-reify
      env/test-lang
      (js/Function.)
      env/inst-some-type
      (oget js/window "document")
      (oget js/window "document" "getElementById")
      (oget env/sample-cljs-fn "__proto__"))))

(deftest test-dollar-preserving-demunge
  (testing "exercise dollar preserving demunging"
    (are [munged demunged] (= (m/dollar-preserving-demunge munged) demunged)
      "" ""
      "hello" "hello"
      "hello$world" "hello$world")))

(deftest test-proper-demunge
  (testing "exercise proper demunging"
    (are [munged demunged] (= (m/proper-demunge munged) demunged)
      "" ""
      "hello" "hello"
      "hello$world" "hello$world"
      "this$" "this"
      "null" "null"
      "null$" "null"
      "null$x" "null$x"
      "$null" "$null"
      "$null$" "$null$")))

(deftest test-proper-arg-demunge
  (testing "exercise proper arg demunging"
    (are [munged demunged] (= (m/proper-arg-demunge munged) demunged)
      "" ""
      "_hello" "_hello"
      "-hello" "_hello"
      "_" "_"
      "-" "_"
      "--x" "_-x")))

(deftest test-parse-fn-info
  (testing "exercise parsing fn infos"
    (are [f expected] (match-seqs? (m/parse-fn-info f) expected)
      env/sample-cljs-fn ["devtools.tests.env.core" "sample-cljs-fn" "var-args"]
      env/cljs-fn-with-vec-destructuring ["devtools.tests.env.core" "cljs-fn-with-vec-destructuring" #"p--\d+"]
      env/cljs-fn-with-vec-destructuring-var ["devtools.tests.env.core" "cljs-fn-with-vec-destructuring-var" "var-args"]
      env/cljs-fn-with-map-destructuring ["devtools.tests.env.core" "cljs-fn-with-map-destructuring" #"p--\d+"]
      env/cljs-fn-with-map-destructuring-var ["devtools.tests.env.core" "cljs-fn-with-map-destructuring-var" "var-args"])))

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
      env/minimal-fn #{}
      env/simplest-fn #{}
      env/clsj-fn-with-fancy-name#$%!? #{}
      env/sample-cljs-fn #{:devtools.munging/variadic}
      env/cljs-fn-with-vec-destructuring #{}
      env/cljs-fn-with-vec-destructuring-var #{:devtools.munging/variadic}
      env/cljs-fn-with-map-destructuring #{}
      env/cljs-fn-with-map-destructuring-var #{:devtools.munging/variadic}
      env/cljs-fn-var #{:devtools.munging/variadic}
      env/cljs-fn-multi-arity #{1 2 4}
      env/cljs-fn-multi-arity-var #{1 2 4 :devtools.munging/variadic}
      env/inst-type-ifn0 #{0}
      env/inst-type-ifn1 #{1}
      env/inst-type-ifn2 #{1 2}
      env/inst-type-ifn2va #{:devtools.munging/variadic}
      env/inst-type-ifn4va #{0 1 4 :devtools.munging/variadic})))

(deftest test-ui-strings
  (testing "exercise args-lists-to-strings"
    (are [f expected] (match-seqs? (m/extract-arities f true " " "..." " & ") expected)
      env/minimal-fn [""]
      env/simplest-fn [""]
      env/clsj-fn-with-fancy-name#$%!? ["arg1! arg? *&mo-re"]
      env/sample-cljs-fn ["p1 p2 & rest"]
      env/cljs-fn-with-vec-destructuring ["p"]
      env/cljs-fn-with-vec-destructuring-var ["& p"]
      env/cljs-fn-with-map-destructuring ["p"]
      env/cljs-fn-with-map-destructuring-var ["& p"]
      env/cljs-fn-var ["first second & rest"]
      env/cljs-fn-multi-arity ["a1" "a2-1 a2-2" "a3-1 a3 a3₂ a3-4"]
      env/cljs-fn-multi-arity-var ["a1" "a2-1 a2-2" "a3-1 a3-2 a3-3 a3-4" "va1 va2 & rest"]
      env/inst-type-ifn0 [""]
      env/inst-type-ifn1 ["p1"]
      env/inst-type-ifn2 ["p1" "p1 p2"]
      env/inst-type-ifn2va ["p1 p2 & rest"]
      env/inst-type-ifn4va ["" "p1" "p p₂ p₃ p₄" "p p₂ p₃ p₄ & p₅"])))

(deftest test-present-function-name
  (let [known-namespaces #{"dirac.tests.scenarios.core_async"
                           "cljs.core.async.impl.channels"
                           "cljs.core.async.impl.timers"
                           "dirac.automation.scenario"
                           "cljs.core.async.impl.ioc_helpers"
                           "cljs.core.async"
                           "cljs.core.async.impl.dispatch"
                           "cljs.core.async.impl.protocols"
                           "devtools.async"}
        ns-detector (fn [ns]
                      (some? (known-namespaces ns)))]
    (testing "exercise presentation of a core-async stack trace (with namespaces)"
      (let [present-opts {:include-ns?               true
                          :include-protocol-ns?      true
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$core_async$break_here_BANG_" "dirac.tests.scenarios.core-async/break-here!"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto____1" "dirac.tests.scenarios.core-async/break-async-$-state-machine--34559--auto----1"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto__" "dirac.tests.scenarios.core-async/break-async-$-state-machine--34559--auto--"
          "cljs$core$async$impl$ioc_helpers$run_state_machine" "cljs.core.async.impl.ioc-helpers/run-state-machine"
          "cljs$core$async$impl$ioc_helpers$run_state_machine_wrapped" "cljs.core.async.impl.ioc-helpers/run-state-machine-wrapped"
          "cljs$core$async$impl$dispatch$process_messages" "cljs.core.async.impl.dispatch/process-messages"
          "devtools$async$promise_based_set_immediate" "devtools.async/promise-based-set-immediate"
          "cljs$core$async$impl$dispatch$queue_dispatcher" "cljs.core.async.impl.dispatch/queue-dispatcher"
          "cljs$core$async$impl$dispatch$run" "cljs.core.async.impl.dispatch/run"
          "cljs.core.async.impl.channels.ManyToManyChannel.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1" "cljs.core.async.impl.protocols.Channel:close!¹ (cljs.core.async.impl.channels/ManyToManyChannel)"
          "cljs$core$async$impl$protocols$close_BANG_" "cljs.core.async.impl.protocols/close!"
          "cljs$core$async$impl$dispatch$queue_delay" "cljs.core.async.impl.dispatch/queue-delay"
          "cljs$core$async$impl$timers$timeout" "cljs.core.async.impl.timers/timeout"
          "cljs$core$async$timeout" "cljs.core.async/timeout"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto____1" "dirac.tests.scenarios.core-async/break-async-$-state-machine--34559--auto----1"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto__" "dirac.tests.scenarios.core-async/break-async-$-state-machine--34559--auto--"
          "cljs$core$async$impl$ioc_helpers$run_state_machine" "cljs.core.async.impl.ioc-helpers/run-state-machine"
          "cljs$core$async$impl$ioc_helpers$run_state_machine_wrapped" "cljs.core.async.impl.ioc-helpers/run-state-machine-wrapped"
          "cljs$core$async$impl$dispatch$process_messages" "cljs.core.async.impl.dispatch/process-messages"
          "devtools$async$promise_based_set_immediate" "devtools.async/promise-based-set-immediate"
          "cljs$core$async$impl$dispatch$queue_dispatcher" "cljs.core.async.impl.dispatch/queue-dispatcher"
          "cljs$core$async$impl$dispatch$run" "cljs.core.async.impl.dispatch/run"
          "dirac$tests$scenarios$core_async$break_async" "dirac.tests.scenarios.core-async/break-async"
          "dirac$tests$scenarios$core_async$break_async_handler" "dirac.tests.scenarios.core-async/break-async-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "dirac.automation.scenario/call-trigger!")))
    (testing "exercise presentation of a core-async stack trace (without namespaces)"
      (let [present-opts {:include-ns?               false
                          :include-protocol-ns?      false
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$core_async$break_here_BANG_" "break-here!"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto____1" "break-async-$-state-machine--34559--auto----1"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto__" "break-async-$-state-machine--34559--auto--"
          "cljs$core$async$impl$ioc_helpers$run_state_machine" "run-state-machine"
          "cljs$core$async$impl$ioc_helpers$run_state_machine_wrapped" "run-state-machine-wrapped"
          "cljs$core$async$impl$dispatch$process_messages" "process-messages"
          "devtools$async$promise_based_set_immediate" "promise-based-set-immediate"
          "cljs$core$async$impl$dispatch$queue_dispatcher" "queue-dispatcher"
          "cljs$core$async$impl$dispatch$run" "run"
          "cljs.core.async.impl.channels.ManyToManyChannel.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1" "Channel:close!¹ (ManyToManyChannel)"
          "cljs$core$async$impl$protocols$close_BANG_" "close!"
          "cljs$core$async$impl$dispatch$queue_delay" "queue-delay"
          "cljs$core$async$impl$timers$timeout" "timeout"
          "cljs$core$async$timeout" "timeout"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto____1" "break-async-$-state-machine--34559--auto----1"
          "dirac$tests$scenarios$core_async$break_async_$_state_machine__34559__auto__" "break-async-$-state-machine--34559--auto--"
          "cljs$core$async$impl$ioc_helpers$run_state_machine" "run-state-machine"
          "cljs$core$async$impl$ioc_helpers$run_state_machine_wrapped" "run-state-machine-wrapped"
          "cljs$core$async$impl$dispatch$process_messages" "process-messages"
          "devtools$async$promise_based_set_immediate" "promise-based-set-immediate"
          "cljs$core$async$impl$dispatch$queue_dispatcher" "queue-dispatcher"
          "cljs$core$async$impl$dispatch$run" "run"
          "dirac$tests$scenarios$core_async$break_async" "break-async"
          "dirac$tests$scenarios$core_async$break_async_handler" "break-async-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "call-trigger!"))))
  (let [known-namespaces #{"dirac.tests.scenarios.breakpoint.core"
                           "dirac.automation.scenario"}
        ns-detector (fn [ns]
                      (some? (known-namespaces ns)))]
    (testing "exercise presentation of a common stack trace (with namespaces)"
      (let [present-opts {:include-ns?               true
                          :include-protocol-ns?      true
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$breakpoint$core$breakpoint_demo" "dirac.tests.scenarios.breakpoint.core/breakpoint-demo"
          "dirac$tests$scenarios$breakpoint$core$breakpoint_demo_handler" "dirac.tests.scenarios.breakpoint.core/breakpoint-demo-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "dirac.automation.scenario/call-trigger!")))
    (testing "exercise presentation of a common stack trace (without namespaces)"
      (let [present-opts {:include-ns?               false
                          :include-protocol-ns?      false
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$breakpoint$core$breakpoint_demo" "breakpoint-demo"
          "dirac$tests$scenarios$breakpoint$core$breakpoint_demo_handler" "breakpoint-demo-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "call-trigger!"))))
  (let [known-namespaces #{"dirac.tests.scenarios.exception.core"
                           "cljs.core"
                           "dirac.automation.scenario"}
        ns-detector (fn [ns]
                      (some? (known-namespaces ns)))]
    (testing "exercise presentation of a non-trivial stack trace (with namespaces)"
      (let [present-opts {:include-ns?               true
                          :include-protocol-ns?      true
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$exception$core$break_BANG_" "dirac.tests.scenarios.exception.core/break!"
          "dirac$tests$scenarios$exception$core$crash_or_break_BANG_" "dirac.tests.scenarios.exception.core/crash-or-break!"
          "dirac.tests.scenarios.exception.core.TestType.dirac$tests$scenarios$exception$core$ITestProtocol$_pmethod$arity$4" "dirac.tests.scenarios.exception.core.ITestProtocol:-pmethod⁴ (dirac.tests.scenarios.exception.core/TestType)"
          "dirac.tests.scenarios.exception.core._pmethod.cljs$core$IFn$_invoke$arity$4" "cljs.core.IFn:-invoke⁴ (dirac.tests.scenarios.exception.core/-pmethod)"
          "dirac$tests$scenarios$exception$core$_pmethod" "dirac.tests.scenarios.exception.core/-pmethod"
          "dirac.tests.scenarios.exception.core.TestType.dirac$tests$scenarios$exception$core$ITestProtocol$_pmethod$arity$3" "dirac.tests.scenarios.exception.core.ITestProtocol:-pmethod³ (dirac.tests.scenarios.exception.core/TestType)"
          "dirac.tests.scenarios.exception.core._pmethod.cljs$core$IFn$_invoke$arity$3" "cljs.core.IFn:-invoke³ (dirac.tests.scenarios.exception.core/-pmethod)"
          "dirac$tests$scenarios$exception$core$_pmethod" "dirac.tests.scenarios.exception.core/-pmethod"
          "dirac$tests$scenarios$exception$core$excercise_protocol_BANG_" "dirac.tests.scenarios.exception.core/excercise-protocol!"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$variadic" "cljs.core.IFn:-invokeⁿ (dirac.tests.scenarios.exception.core/multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "dirac.tests.scenarios.exception.core/multi-arity-fn"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$2" "cljs.core.IFn:-invoke² (dirac.tests.scenarios.exception.core/multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "dirac.tests.scenarios.exception.core/multi-arity-fn"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$0" "cljs.core.IFn:-invoke⁰ (dirac.tests.scenarios.exception.core/multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "dirac.tests.scenarios.exception.core/multi-arity-fn"
          "dirac$tests$scenarios$exception$core$fancy_$_PERCENT_$_SHARP__PERCENT_$_SHARP__function_QMARK__QMARK__QMARK__name" "dirac.tests.scenarios.exception.core/fancy-$%$#%$#-function???-name"
          "dirac$tests$scenarios$exception$core$breakpoint_demo_BANG_" "dirac.tests.scenarios.exception.core/breakpoint-demo!"
          "dirac$tests$scenarios$exception$core$breakpoint_demo_handler" "dirac.tests.scenarios.exception.core/breakpoint-demo-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "dirac.automation.scenario/call-trigger!")))
    (testing "exercise presentation of a non-trivial stack trace (without namespaces)"
      (let [present-opts {:include-ns?               false
                          :include-protocol-ns?      false
                          :silence-common-protocols? false
                          :ns-detector               ns-detector}]
        (are [munged-name expected] (= (m/present-function-name munged-name present-opts) expected)
          "dirac$tests$scenarios$exception$core$break_BANG_" "break!"
          "dirac$tests$scenarios$exception$core$crash_or_break_BANG_" "crash-or-break!"
          "dirac.tests.scenarios.exception.core.TestType.dirac$tests$scenarios$exception$core$ITestProtocol$_pmethod$arity$4" "ITestProtocol:-pmethod⁴ (TestType)"
          "dirac.tests.scenarios.exception.core._pmethod.cljs$core$IFn$_invoke$arity$4" "IFn:-invoke⁴ (-pmethod)"
          "dirac$tests$scenarios$exception$core$_pmethod" "-pmethod"
          "dirac.tests.scenarios.exception.core.TestType.dirac$tests$scenarios$exception$core$ITestProtocol$_pmethod$arity$3" "ITestProtocol:-pmethod³ (TestType)"
          "dirac.tests.scenarios.exception.core._pmethod.cljs$core$IFn$_invoke$arity$3" "IFn:-invoke³ (-pmethod)"
          "dirac$tests$scenarios$exception$core$_pmethod" "-pmethod"
          "dirac$tests$scenarios$exception$core$excercise_protocol_BANG_" "excercise-protocol!"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$variadic" "IFn:-invokeⁿ (multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "multi-arity-fn"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$2" "IFn:-invoke² (multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "multi-arity-fn"
          "dirac.tests.scenarios.exception.core.multi_arity_fn.cljs$core$IFn$_invoke$arity$0" "IFn:-invoke⁰ (multi-arity-fn)"
          "dirac$tests$scenarios$exception$core$multi_arity_fn" "multi-arity-fn"
          "dirac$tests$scenarios$exception$core$fancy_$_PERCENT_$_SHARP__PERCENT_$_SHARP__function_QMARK__QMARK__QMARK__name" "fancy-$%$#%$#-function???-name"
          "dirac$tests$scenarios$exception$core$breakpoint_demo_BANG_" "breakpoint-demo!"
          "dirac$tests$scenarios$exception$core$breakpoint_demo_handler" "breakpoint-demo-handler"
          "dirac$automation$scenario$call_trigger_BANG_" "call-trigger!")))))
