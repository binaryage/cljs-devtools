(defproject binaryage/devtools "0.9.2"
  :description "A collection of Chrome DevTools enhancements for ClojureScript developers."
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}

  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.473" :scope "provided"]
                 [binaryage/env-config "0.1.1"]]

  :clean-targets ^{:protect false} ["target"
                                    "test/resources/.compiled"]

  :plugins [[lein-cljsbuild "1.1.5"]
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
              {:dependencies   ~(let [project (->> "project.clj" slurp read-string (drop 3) (apply hash-map))
                                      test-dep? #(->> % (drop 2) (apply hash-map) :scope (= "test"))
                                      non-test-deps (remove test-dep? (:dependencies project))]
                                  (with-meta (vec non-test-deps) {:replace true}))                                            ; so ugly!
               :source-paths   ^:replace ["src/lib"]
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
             {:cljsbuild {:builds {:tests
                                   {:source-paths ["src/lib"
                                                   "test/src/tests"]
                                    :compiler     {:output-to     "test/resources/.compiled/tests/build.js"
                                                   :output-dir    "test/resources/.compiled/tests"
                                                   :asset-path    ".compiled/tests"
                                                   :main          devtools.main
                                                   :optimizations :none}}
                                   :tests-with-config
                                   {:source-paths ["src/lib"
                                                   "test/src/tests"]
                                    :compiler     {:output-to       "test/resources/.compiled/tests-with-config/build.js"
                                                   :output-dir      "test/resources/.compiled/tests-with-config"
                                                   :asset-path      ".compiled/tests-with-config"
                                                   :main            devtools.main
                                                   :optimizations   :none
                                                   :external-config {:devtools/config {:features-to-install    [:hints]
                                                                                       :fn-symbol              "F"
                                                                                       :print-config-overrides true}}
                                                   :preloads        [devtools.preload]}}                                      ; CLJS-1688
                                   :dead-code
                                   {:source-paths ["src/lib"
                                                   "test/src/dead-code"]
                                    :compiler     {:output-to       "test/resources/.compiled/dead-code/build.js"
                                                   :output-dir      "test/resources/.compiled/dead-code"
                                                   :asset-path      ".compiled/dead-code"
                                                   :main            devtools.main
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}

                                   :advanced-warning
                                   {:source-paths ["src/lib"
                                                   "test/src/advanced-warning"]
                                    :compiler     {:output-to     "test/resources/.compiled/advanced-warning/build.js"
                                                   :output-dir    "test/resources/.compiled/advanced-warning"
                                                   :asset-path    ".compiled/advanced-warning"
                                                   :main          devtools.main
                                                   :pseudo-names  true
                                                   :optimizations :advanced}}}}}
             :auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]}}}}

             :adhoc-auto-testing
             {:cljsbuild {:builds {:tests
                                   {:notify-command ["phantomjs" "test/resources/phantom.js" "test/resources/run-tests-adhoc.html"]}}}}}

  :aliases {"test"                   ["do"
                                      ["clean"]
                                      ["test-tests"]
                                      ["test-tests-with-config"]
                                      ["test-dead-code"]
                                      ["test-advanced-warning"]]
            "test-dead-code"         ["do"
                                      ["with-profile" "+testing" "cljsbuild" "once" "dead-code"]
                                      ["shell" "test/scripts/dead-code-check.sh"]]
            "test-tests"             ["do"
                                      ["with-profile" "+testing" "cljsbuild" "once" "tests"]
                                      ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests.html"]]
            "test-tests-with-config" ["do"
                                      ["shell" "scripts/compile-tests-with-config.sh"]
                                      ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-tests-with-config.html"]]
            "test-advanced-warning"  ["do"
                                      ["with-profile" "+testing" "cljsbuild" "once" "advanced-warning"]
                                      ["shell" "phantomjs" "test/resources/phantom.js" "test/resources/run-advanced-warning.html"]]
            "auto-test"              ["do"
                                      ["clean"]
                                      ["with-profile" "+testing,+auto-testing" "cljsbuild" "auto" "tests"]]
            "adhoc-auto-test"        ["do"
                                      ["clean"]
                                      ["with-profile" "+testing,+adhoc-auto-testing" "cljsbuild" "auto" "tests"]]
            "install"                ["do"
                                      ["shell" "scripts/prepare-jar.sh"]
                                      ["shell" "scripts/local-install.sh"]]
            "jar"                    ["shell" "scripts/prepare-jar.sh"]
            "deploy"                 ["shell" "scripts/deploy-clojars.sh"]
            "release"                ["do"
                                      ["clean"]
                                      ["shell" "scripts/check-versions.sh"]
                                      ["shell" "scripts/prepare-jar.sh"]
                                      ["shell" "scripts/check-release.sh"]
                                      ["shell" "scripts/deploy-clojars.sh"]]})
