(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [binaryage/devtools "0.5.4"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [cljs-http "0.1.39"]
                 [environ "1.0.2"]
                 [figwheel "0.5.1"]]

  ;:jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.1"]
            [lein-shell "0.5.0"]
            [lein-environ "1.0.2"]]

  ; =========================================================================================================================

  :figwheel {:server-port    7000
             :server-logfile ".figwheel_server.log"}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target"]

  ; =========================================================================================================================

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {; --------------------------------------------------------------------------------------------------------------
             :demo
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/demo"]
                                    :compiler     {:output-to     "resources/public/_compiled/demo/devtools_sample.js"
                                                   :output-dir    "resources/public/_compiled/demo"
                                                   :asset-path    "_compiled/demo"
                                                   :optimizations :none
                                                   :source-map    true}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :demo-advanced
             {:cljsbuild {:builds {:demo-advanced
                                   {:source-paths ["src/demo"]
                                    :compiler     {:output-to     "resources/public/_compiled/demo_advanced/devtools_sample.js"
                                                   :output-dir    "resources/public/_compiled/demo_advanced"
                                                   :asset-path    "_compiled/demo_advanced"
                                                   :pseudo-names  true
                                                   :optimizations :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-unconditional-install
             {:cljsbuild {:builds {:advanced-unconditional-install
                                   {:source-paths ["src/advanced-unconditional-install"]
                                    :compiler     {:output-to       "resources/public/_compiled/advanced-unconditional-install/devtools_sample.js"
                                                   :output-dir      "resources/public/_compiled/advanced-unconditional-install"
                                                   :asset-path      "_compiled/advanced-unconditional-install"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-conditional-install
             {:cljsbuild {:builds {:advanced-conditional-install
                                   {:source-paths ["src/advanced-conditional-install"]
                                    :compiler     {:output-to       "resources/public/_compiled/advanced-conditional-install/devtools_sample.js"
                                                   :output-dir      "resources/public/_compiled/advanced-conditional-install"
                                                   :asset-path      "_compiled/advanced-conditional-install"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-no-install
             {:cljsbuild {:builds {:advanced-no-install
                                   {:source-paths ["src/advanced-no-install"]
                                    :compiler     {:output-to       "resources/public/_compiled/advanced-no-install/devtools_sample.js"
                                                   :output-dir      "resources/public/_compiled/advanced-no-install"
                                                   :asset-path      "_compiled/advanced-no-install"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :checkouts
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["checkouts/cljs-devtools/src"]}}}}

             ; --------------------------------------------------------------------------------------------------------------
             :debug
             {:env {:devtools-debug "true"}}

             ; --------------------------------------------------------------------------------------------------------------
             :figwheel
             {:cljsbuild {:builds {:demo
                                   {:figwheel true}}}}

             ; --------------------------------------------------------------------------------------------------------------
             :devel
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/debug"
                                                   "checkouts/cljs-devtools/src-debug"]}}}}}

  ; =========================================================================================================================

  :aliases {"demo"                           ["with-profile" "+demo,+figwheel" "figwheel"]
            "cljs"                           ["with-profile" "+demo" "cljsbuild" "auto"]
            "demo-advanced"                  ["with-profile" "+demo-advanced,+checkouts""cljsbuild" "once"]
            "dirac"                          ["with-profile" "+demo,+figwheel" "figwheel"]
            "advanced-unconditional-install" ["with-profile" "+advanced-unconditional-install" "cljsbuild" "once"]
            "advanced-conditional-install"   ["with-profile" "+advanced-conditional-install" "cljsbuild" "once"]
            "advanced-no-install"            ["with-profile" "+advanced-no-install" "cljsbuild" "once"]
            "advanced-compare"               ["do"
                                              "clean,"
                                              "advanced-unconditional-install,"
                                              "advanced-conditional-install,"
                                              "advanced-no-install,"
                                              "shell" "scripts/compare-advanced-builds.sh"]
            "debug"                          ["with-profile" "+demo,+checkouts,+devel,+debug,+figwheel" "figwheel"]})
