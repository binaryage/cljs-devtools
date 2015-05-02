(defproject devtools-sample "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [binaryage/devtools "0.2.1"]
                 [cljs-http "0.1.30"]
                 [ring "1.3.2"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-ring "0.9.1"]]

  :clean-targets ["out"]

  :ring {:handler server.core/app}

  :source-paths ["src" "target/classes"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "checkouts/cljs-devtools/src"]
              :compiler {
                :main devtools-sample.core
                :output-to "out/devtools_sample.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}]})
