(defproject binaryage/devtools "0.5.5-SNAPSHOT"
  :description "Chrome DevTools utilities for ClojureScript"
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}

  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [environ "1.0.2"]]

  :clean-targets ^{:protect false} ["target"
                                    "test/resources/_compiled"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-shell "0.5.0"]]

  :hooks [leiningen.cljsbuild]

  :source-paths ["src"]
  :test-paths ["test/src/test"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:devel
             {:cljsbuild {:builds {:devel
                                   {:source-paths ["src"
                                                   "src-debug"]
                                    :compiler     {:output-to     "target/devel/cljs_devtools.js"
                                                   :output-dir    "target/devel"
                                                   :optimizations :none
                                                   :source-map    true}}}}}

             :testing
             {:cljsbuild {:builds {:tests
                                   {:source-paths ["src"
                                                   "test/src/tests"]
                                    :compiler     {:output-to     "test/resources/_compiled/tests/build.js"
                                                   :output-dir    "test/resources/_compiled/tests"
                                                   :asset-path    "_compiled/tests"
                                                   :main          devtools.runner
                                                   :optimizations :none
                                                   :pretty-print  true
                                                   :source-map    true}}
                                   :dead-code-elimination
                                   {:source-paths ["src"
                                                   "test/src/dead-code-elimination"]
                                    :compiler     {:output-to       "test/resources/_compiled/dead-code-elimination/build.js"
                                                   :output-dir      "test/resources/_compiled/dead-code-elimination"
                                                   :asset-path      "_compiled/dead-code-elimination"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}}

  :aliases {"test"           ["do"
                              "test-phantom,"
                              "test-dead-code"]
            "test-dead-code" ["do"
                              "with-profile" "+testing" "cljsbuild" "once" "dead-code-elimination,"
                              "shell" "test/scripts/dead-code-check.sh"]
            "test-phantom"   ["do"
                              "with-profile" "+testing" "cljsbuild" "once" "tests,"
                              "shell" "phantomjs" "test/resources/phantom.js" "test/resources/runner.html"]
            "release"        ["do"
                              "shell" "scripts/check-versions.sh,"
                              "clean,"
                              "test,"
                              "jar,"
                              "shell" "scripts/check-release.sh,"
                              "deploy clojars"]})