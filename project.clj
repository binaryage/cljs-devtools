(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [binaryage/devtools "0.5.2"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [cljs-http "0.1.39"]
                 [environ "1.0.1"]
                 [figwheel "0.5.0-5"]]

  ;:jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-5"]
            [lein-shell "0.4.2"]
            [lein-environ "1.0.1"]]

  ; =========================================================================================================================

  :figwheel {:server-port    7000
             :server-logfile ".figwheel_server.log"}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target"]

  ; =========================================================================================================================

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {; --------------------------------------------------------------------------------------------------------------
             :dev
             {:dependencies [[binaryage/dirac "0.1.2"]]
              :repl-options {:port             8230
                             :nrepl-middleware [dirac.nrepl.middleware/dirac-repl]
                             :init             (do
                                                 (require 'dirac.agent)
                                                 (dirac.agent/boot!))}}

             ; --------------------------------------------------------------------------------------------------------------
             ; reference piggieback + weasel configuration
             :piggieback
             {:dependencies [[org.clojure/tools.nrepl "0.2.12"]
                             [com.cemerick/piggieback "0.2.1"]
                             [weasel "0.7.0" :exclusions [org.clojure/clojurescript]]]
              :repl-options {:port             8530
                             :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                             :init             (do
                                                 (require 'weasel.repl.websocket)
                                                 (import '(java.util TimerTask Timer))
                                                 (let [repl-env (weasel.repl.websocket/repl-env :ip "0.0.0.0" :port 9001)
                                                       start-repl (fn [] (cemerick.piggieback/cljs-repl repl-env))
                                                       print-usage (fn [] (println "copy&paste this:\n(cemerick.piggieback/cljs-repl (weasel.repl.websocket/repl-env :ip \"0.0.0.0\" :port 9001))"))
                                                       task (proxy [TimerTask] [] (run []                                     ; http://stackoverflow.com/a/16385066/84283
                                                                                    (print-usage)
                                                                                    #_(start-repl)))]                         ; doesn't work for some reason
                                                   (. (new Timer) (schedule task (long 2000)))))}}

             ; --------------------------------------------------------------------------------------------------------------
             :demo
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/demo"]
                                    :compiler     {:output-to     "resources/public/_compiled/demo/devtools_sample.js"
                                                   :output-dir    "resources/public/_compiled/demo"
                                                   :asset-path    "_compiled/demo"
                                                   :optimizations :none
                                                   :source-map    true}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :checkouts
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["checkouts/cljs-devtools/src"]}}}}

             ; --------------------------------------------------------------------------------------------------------------
             :debug
             {:env {:devtools-debug       true
                    :dirac-weasel-verbose true
                    :dirac-agent-verbose  true}}

             ; --------------------------------------------------------------------------------------------------------------
             :figwheel
             {:cljsbuild {:builds {:demo
                                   {:figwheel true}}}}

             ; --------------------------------------------------------------------------------------------------------------
             :weasel
             {:env       {:devtools-weasel true}
              :cljsbuild {:builds {:demo
                                   {:source-paths ["src/weasel"]}}}}

             ; --------------------------------------------------------------------------------------------------------------
             :devel
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/debug"
                                                   "checkouts/cljs-devtools/src-debug"]}}}}}

  ; =========================================================================================================================

  :aliases {"demo"            ["with-profile" "+demo,+figwheel"
                               "do" "clean,"
                               "figwheel"]
            "cljs"            ["with-profile" "+demo"
                               "do" "clean,"
                               "cljsbuild" "auto"]
            "dirac"           ["with-profile" "+demo,+figwheel"
                               "do" "clean,"
                               "figwheel"]
            "piggieback"      ["with-profile" "+demo,+figwheel,-dev,+piggieback,+weasel"
                               "do" "clean,"
                               "figwheel"]
            "piggieback-repl" ["with-profile" "+demo,+figwheel,-dev,+piggieback,+weasel"
                               "repl"]
            "debug"           ["with-profile" "+demo,+checkouts,+devel,+debug,+figwheel"
                               "figwheel"]})
