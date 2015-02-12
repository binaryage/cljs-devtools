(defproject com.binaryage/devtools "0.0-SNAPSHOT"
  :description "Experimental Chrome devtools support for ClojureScript"
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.6.0" :scope "provided"]
                 [org.clojure/clojurescript "0.0-2816" :scope "provided"]]

  :clean-targets ["out"]
  :plugins [[lein-cljsbuild "1.0.4"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src"]
     :compiler     {:output-to      "out/dev/cljs_devtools.js"
                    :output-dir     "out/dev"
                    :optimizations  :none
                    :cache-analysis true
                    :source-map     true}}
    :prod
    {:source-paths ["src"]
     :compiler     {:output-to      "out/prod/cljs_devtools.min.js"
                    :output-dir     "out/prod"
                    :optimizations  :advanced
                    :cache-analysis true
                    :source-map     "out/prod/cljs_devtools.min.js"}}
    :test
    {:source-paths ["src", "test"]
     :compiler     {:output-to     "out/test/cljs_devtools.test.js"
                    :output-dir    "out/test"
                    :optimizations :whitespace
                    :pretty-print  true}}
    }
   :test-commands {"unit" ["phantomjs" "test/phantom.js" "test/runner.html"]}
   }

  )

