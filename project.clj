(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [binaryage/devtools "0.4.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [cljs-http "0.1.38"]
                 [ring "1.4.0"]
                 [environ "1.0.1"]
                 [figwheel "0.5.0-2"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]
            [lein-shell "0.4.2"]
            [lein-ring "0.9.7"]
            [lein-environ "1.0.1"]]

  :ring {:handler      devtools-sample.server/app
         :auto-reload? true}

  :figwheel {:server-port    7000
             :server-logfile ".figwheel_server.log"}

  :source-paths ["src"
                 "src/server"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target"]

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
                                   {:source-paths ["checkouts/cljs-devtools/src"
                                                   "checkouts/figwheel-support/src"]}}}}
             :debug
             {:env {:devtools-debug true}}

             :figwheel
             {:env {:devtools-figwheel true}}

             :devel
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/debug"
                                                   "src/figwheel"
                                                   "checkouts/cljs-devtools/src-debug"]}}}}}

  :aliases {"demo"         ["with-profile" "+demo" "do" "clean," "cljsbuild" "once," "ring" "server"]
            "cljs"         ["with-profile" "+demo" "do" "clean," "cljsbuild" "auto"]
            "dirac"        ["with-profile" "+demo,+checkouts,+devel,+figwheel" "do" "clean," "figwheel"]
            "server"       ["ring" "server"]
            "prepare-checkouts" ["shell" "scripts/prepare-checkouts.sh"]
            "debug"        ["with-profile" "+demo,+checkouts,+devel,+debug,+figwheel" "figwheel"]
            "debug-server" ["with-profile" "+debug" "ring" "server"]})
