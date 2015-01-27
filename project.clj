(defproject cljs-devtools-sample "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2725"]]

  :node-dependencies [[source-map-support "0.2.8"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-npm "0.4.0"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out/cljs_devtools_sample" "cljs_devtools_sample.js"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :main cljs-devtools-sample.core
                :output-to "out/cljs_devtools_sample.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}]})
