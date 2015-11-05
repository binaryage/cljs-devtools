(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [binaryage/devtools "0.4.1"]
                 [cljs-http "0.1.37"]
                 [ring "1.4.0"]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-ring "0.9.7"]]

  :clean-targets ^{:protect false} ["resources/public/_compiled"]

  :ring {:handler server.core/app}

  :source-paths ["src" "target/classes"]

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"
                                       ;"checkouts/cljs-devtools/src"
                                       ;"checkouts/cljs-devtools/src-debug"
                                       ]
                        :compiler     {:main           devtools-sample.core
                                       :output-to      "resources/public/_compiled/devtools_sample.js"
                                       :output-dir     "resources/public/_compiled"
                                       :asset-path     "_compiled"
                                       :optimizations  :none
                                       :source-map     true}}]})
