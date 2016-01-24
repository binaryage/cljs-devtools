(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [binaryage/devtools "0.5.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [cljs-http "0.1.39"]
                 [environ "1.0.1"]
                 [figwheel "0.5.0-4"]]

  ;:jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-4"]
            [lein-shell "0.4.2"]
            [lein-environ "1.0.1"]]

  :figwheel {:server-port    7000
             :server-logfile ".figwheel_server.log"}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.3.1"]
                                  [clj-logging-config "1.9.12"]
                                  [http-kit "2.1.21-alpha2"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [binaryage/dirac "0.1.0"]]
                   :repl-options {:port             8230
                                  :nrepl-middleware [dirac.nrepl.middleware/dirac-repl]
                                  :init             (do
                                                      (require 'dirac.agent)
                                                      (dirac.agent/boot!))}}

             :demo
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

             :figwheel
                  {:env       {:devtools-figwheel true}
                   :cljsbuild {:builds {:demo
                                        {:figwheel true}}}}

             :devel
                  {:cljsbuild {:builds {:demo
                                        {:source-paths ["src/debug"
                                                        "checkouts/cljs-devtools/src-debug"]}}}}}

  :aliases {"demo"              ["with-profile" "+demo,+figwheel"
                                 "do" "clean,"
                                 "figwheel"]
            "cljs"              ["with-profile" "+demo"
                                 "do" "clean,"
                                 "cljsbuild" "auto"]
            "dirac"             ["with-profile" "+demo,+figwheel"
                                 "do" "clean,"
                                 "figwheel"]
            "debug"             ["with-profile" "+demo,+checkouts,+devel,+debug,+figwheel"
                                 "figwheel"]})
