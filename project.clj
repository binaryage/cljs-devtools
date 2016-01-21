(defproject binaryage/devtools "0.5.2-SNAPSHOT"
  :description "Experimental Chrome devtools support for ClojureScript"
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}

  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [environ "1.0.1" :scope "provided"]]

  :clean-targets ^{:protect false} ["target"
                                    "test/_generated"]

  :plugins [[lein-cljsbuild "1.1.1"]]
  :hooks [leiningen.cljsbuild]

  :source-paths ["src"]
  :test-paths ["test"]

  :aliases {"test" ["with-profile" "test" "test"]}

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:devel
             {:cljsbuild {:builds {:devel
                                   {:source-paths ["src", "src-debug"]
                                    :compiler     {:output-to     "target/devel/cljs_devtools.js"
                                                   :output-dir    "target/devel"
                                                   :optimizations :none
                                                   :source-map    true}}}}}

             :release
             {:cljsbuild {:builds {:release
                                   {:source-paths ["src"]
                                    :compiler     {:output-to     "target/release/cljs_devtools.min.js"
                                                   :output-dir    "target/release"
                                                   :optimizations :advanced
                                                   :source-map    "out/prod/cljs_devtools.min.js.map"}}}}}

             :test
             {:cljsbuild {:builds        {:test
                                          {:source-paths ["src", "test"]
                                           :compiler     {:output-to     "test/_generated/cljs_devtools.test.js"
                                                          :output-dir    "test/_generated"
                                                          :main          devtools.runner
                                                          :asset-path    "_generated"
                                                          :optimizations :none
                                                          :pretty-print  true
                                                          :source-map    true}}}
                          :test-commands {"unit" ["phantomjs" "test/phantom.js" "test/runner.html"]}}}}
  )