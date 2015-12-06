(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [binaryage/devtools "0.4.1"]
                 [cljs-http "0.1.38"]
                 [ring "1.4.0"]
                 [ring-refresh "0.1.1"]
                 [environ "1.0.1"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-ring "0.9.7"]
            [lein-environ "1.0.1"]
            [lein-cooper "0.0.1"]]

  :ring {:handler       devtools-sample.server/app
         :auto-reload?  true
         :auto-refresh? true}

  :source-paths ["src/server"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target/classes"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:demo
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/demo"]
                                    :compiler     {:output-to     "resources/public/_compiled/demo/devtools_sample.js"
                                                   :output-dir    "resources/public/_compiled/demo"
                                                   :asset-path    "_compiled/demo"
                                                   :optimizations :none
                                                   :source-map    true}}}}}
             :checkouts
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["checkouts/cljs-devtools/src"]}}}}

             :debug
             {:env {:devtools-debug true}}

             :devel
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/debug"
                                                   "checkouts/cljs-devtools/src-debug"]}}}}}

  :aliases {"demo"   ["with-profile" "+demo" "do" "clean," "cljsbuild" "once," "ring" "server"]
            "devel"  ["cooper"]
            "server" ["with-profile" "+demo" "ring" "server"]})
