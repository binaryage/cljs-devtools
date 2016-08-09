(defproject binaryage/devtools "0.8.2-SNAPSHOT"
  :description "A collection of Chrome DevTools enhancements for ClojureScript developers."
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}

  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.89" :scope "provided"]]

  :clean-targets ^{:protect false} ["target"
                                    "test/resources/_compiled"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-shell "0.5.0"]]

  :source-paths ["src/lib"
                 "src/debug"]

  :test-paths ["test"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:devel
             {:cljsbuild {:builds {:devel
                                   {:source-paths ["src/lib"
                                                   "src/debug"]
                                    :compiler     {:output-to     "target/devel/cljs_devtools.js"
                                                   :output-dir    "target/devel"
                                                   :optimizations :none}}}}}

             :testing
             {:cljsbuild {:builds {:tests
                                   {:source-paths ["src/lib"
                                                   "test/src/tests"]
                                    :compiler     {:output-to     "test/resources/_compiled/tests/build.js"
                                                   :output-dir    "test/resources/_compiled/tests"
                                                   :asset-path    "_compiled/tests"
                                                   :main          devtools.main
                                                   :optimizations :none}}
                                   :tests-with-config
                                   {:source-paths ["src/lib"
                                                   "test/src/tests"]
                                    :compiler     {:output-to       "test/resources/_compiled/tests-with-config/build.js"
                                                   :output-dir      "test/resources/_compiled/tests-with-config"
                                                   :asset-path      "_compiled/tests-with-config"
                                                   :main            devtools.main
                                                   :optimizations   :none
                                                   :external-config {:devtools/config {:features-to-install    [:hints]
                                                                                       :fn-symbol              "F"
                                                                                       :print-config-overrides true}}
                                                   :preloads        [devtools.preload]}}                                      ; CLJS-1688
                                   :dead-code
                                   {:source-paths ["src/lib"
                                                   "test/src/dead-code"]
                                    :compiler     {:output-to       "test/resources/_compiled/dead-code/build.js"
                                                   :output-dir      "test/resources/_compiled/dead-code"
                                                   :asset-path      "_compiled/dead-code"
                                                   :main            devtools.main
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             :auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]}}}}

             :adhoc-auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests-adhoc.html"]}}}}}

  :aliases {"test"                   ["do"
                                      "clean,"
                                      "test-tests,"
                                      "test-tests-with-config,"
                                      "test-dead-code"]
            "test-dead-code"         ["do"
                                      "with-profile" "+testing" "cljsbuild" "once" "dead-code,"
                                      "shell" "test/scripts/dead-code-check.sh"]
            "test-tests"             ["do"
                                      "with-profile" "+testing" "cljsbuild" "once" "tests,"
                                      "shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]
            "test-tests-with-config" ["do"
                                      "with-profile" "+testing" "cljsbuild" "once" "tests-with-config,"
                                      "shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests-with-config.html"]
            "auto-test"              ["do"
                                      "clean,"
                                      "with-profile" "+testing,+auto-testing" "cljsbuild" "auto" "tests"]
            "adhoc-auto-test"        ["do"
                                      "clean,"
                                      "with-profile" "+testing,+adhoc-auto-testing" "cljsbuild" "auto" "tests"]
            "release"                ["do"
                                      "shell" "scripts/check-versions.sh,"
                                      "clean,"
                                      "test,"
                                      "jar,"
                                      "shell" "scripts/check-release.sh,"
                                      "deploy" "clojars"]})
