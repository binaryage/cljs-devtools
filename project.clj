(def clojurescript-version (or (System/getenv "CANARY_CLOJURESCRIPT_VERSION") "1.10.597"))
(defproject binaryage/devtools "0.9.11"
  :description "A collection of Chrome DevTools enhancements for ClojureScript developers."
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}

  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript ~clojurescript-version :scope "provided"]]

  :clean-targets ^{:protect false} ["target"
                                    "test/resources/.compiled"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-shell "0.5.0"]]

  ; this is for IntelliJ + Cursive to play well
  :source-paths ["src/lib"
                 "src/debug"]
  :test-paths ["test/src"]
  :resource-paths ["test/resources"
                   "scripts"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:nuke-aliases
             {:aliases ^:replace {}}

             :lib
             ^{:pom-scope :provided}                                                                                          ; ! to overcome default jar/pom behaviour, our :dependencies replacement would be ignored for some reason
             [:nuke-aliases
              {:source-paths   ^:replace ["src/lib"]
               :resource-paths ^:replace []
               :test-paths     ^:replace []}]

             :devel
             {:cljsbuild {:builds {:devel
                                   {:source-paths ["src/lib"
                                                   "src/debug"]
                                    :compiler     {:output-to     "target/devel/cljs_devtools.js"
                                                   :output-dir    "target/devel"
                                                   :optimizations :none}}}}}

             :testing
             {:source-paths   ^:replace []
              :resource-paths ^:replace []
              :test-paths     ^:replace []
              :cljsbuild      {:builds {:tests
                                        {:source-paths ["src/lib"
                                                        "test/src/tests"]
                                         :compiler     {:output-to      "test/resources/.compiled/tests/build.js"
                                                        :output-dir     "test/resources/.compiled/tests"
                                                        :asset-path     ".compiled/tests"
                                                        :main           devtools.main
                                                        :preloads       [devtools.testenv]
                                                        :optimizations  :none
                                                        :checked-arrays :warn}}
                                        :dead-code
                                        {:source-paths ["src/lib"
                                                        "test/src/dead-code"]
                                         :compiler     {:output-to       "test/resources/.compiled/dead-code/build.js"
                                                        :output-dir      "test/resources/.compiled/dead-code"
                                                        :asset-path      ".compiled/dead-code"
                                                        :main            devtools.main
                                                        :closure-defines {"goog.DEBUG" false}
                                                        :pseudo-names    true
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :dce-with-debug
                                        {:source-paths ["src/lib"
                                                        "test/src/dead-code"]
                                         :compiler     {:output-to       "test/resources/.compiled/dce-with-debug/build.js"
                                                        :output-dir      "test/resources/.compiled/dce-with-debug"
                                                        :asset-path      ".compiled/dce-with-debug"
                                                        :main            devtools.main
                                                        :closure-defines {"goog.DEBUG" true}
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :dce-no-debug
                                        {:source-paths ["src/lib"
                                                        "test/src/dead-code"]
                                         :compiler     {:output-to       "test/resources/.compiled/dce-no-debug/build.js"
                                                        :output-dir      "test/resources/.compiled/dce-no-debug"
                                                        :asset-path      ".compiled/dce-no-debug"
                                                        :main            devtools.main
                                                        :closure-defines {"goog.DEBUG" false}
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :dce-no-mention
                                        {:source-paths ["src/lib"
                                                        "test/src/dead-code-no-mention"]
                                         :compiler     {:output-to       "test/resources/.compiled/dce-no-mention/build.js"
                                                        :output-dir      "test/resources/.compiled/dce-no-mention"
                                                        :asset-path      ".compiled/dce-no-mention"
                                                        :main            devtools.main
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :dce-no-require
                                        {:source-paths ["src/lib"
                                                        "test/src/dead-code-no-require"]
                                         :compiler     {:output-to       "test/resources/.compiled/dce-no-require/build.js"
                                                        :output-dir      "test/resources/.compiled/dce-no-require"
                                                        :asset-path      ".compiled/dce-no-require"
                                                        :main            devtools.main
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :dce-no-sources
                                        {:source-paths ["test/src/dead-code-no-require"]
                                         :compiler     {:output-to       "test/resources/.compiled/dce-no-sources/build.js"
                                                        :output-dir      "test/resources/.compiled/dce-no-sources"
                                                        :asset-path      ".compiled/dce-no-sources"
                                                        :main            devtools.main
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}

                                        :advanced-warning
                                        {:source-paths ["src/lib"
                                                        "test/src/advanced-warning"]
                                         :compiler     {:output-to       "test/resources/.compiled/advanced-warning/build.js"
                                                        :output-dir      "test/resources/.compiled/advanced-warning"
                                                        :asset-path      ".compiled/advanced-warning"
                                                        :main            devtools.main
                                                        :external-config {:devtools/config {:silence-optimizations-warning true}}
                                                        :optimizations   :advanced
                                                        :checked-arrays  :warn}}}}}

             :dce-pseudo-names
             {:cljsbuild {:builds {:dce-with-debug {:compiler {:pseudo-names true}}
                                   :dce-no-debug   {:compiler {:pseudo-names true}}
                                   :dce-no-mention {:compiler {:pseudo-names true}}
                                   :dce-no-require {:compiler {:pseudo-names true}}
                                   :dce-no-sources {:compiler {:pseudo-names true}}}}}

             :auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]}}}}

             :adhoc-auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests-adhoc.html"]}}}}}

  :aliases {"test"                                ["do"
                                                   ["clean"]
                                                   ["test-tests"]
                                                   ["test-dead-code"]
                                                   ;["test-dce-size"]
                                                   ["test-advanced-warning"]]
            "test-dead-code"                      ["do"
                                                   ["with-profile" "+testing" "cljsbuild" "once" "dead-code"]
                                                   ["shell" "test/scripts/dead-code-check.sh"]]
            "test-dce-size"                       ["shell" "scripts/check-dce-size.sh" "+testing"]
            "compare-dead-code"                   ["shell" "scripts/compare-dead-code.sh" "+testing"]
            "compare-dead-code-with-pseudo-names" ["shell" "scripts/compare-dead-code.sh" "+testing,+dce-pseudo-names"]
            "test-tests"                          ["do"
                                                   ["with-profile" "+testing" "cljsbuild" "once" "tests"]
                                                   ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]]
            "test-tests-with-config"              ["do"
                                                   ["shell" "scripts/compile-tests-with-config.sh"]
                                                   ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests-with-config.html"]]
            "test-advanced-warning"               ["do"
                                                   ["with-profile" "+testing" "cljsbuild" "once" "advanced-warning"]
                                                   ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-advanced-warning.html"]]
            "auto-test"                           ["do"
                                                   ["clean"]
                                                   ["with-profile" "+testing,+auto-testing" "cljsbuild" "auto" "tests"]]
            "adhoc-auto-test"                     ["do"
                                                   ["clean"]
                                                   ["with-profile" "+testing,+adhoc-auto-testing" "cljsbuild" "auto" "tests"]]
            "release"                             ["shell" "scripts/release.sh"]})
