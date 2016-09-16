(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools-sample"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [binaryage/devtools "0.8.2"]
                 [binaryage/dirac "0.6.6"]
                 [com.cognitect/transit-clj "0.8.288"]
                 [cljs-http "0.1.41"]
                 [environ "1.1.0"]
                 [figwheel "0.5.7"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.7"]
            [lein-shell "0.5.0"]
            [lein-environ "1.1.0"]]

  ; =========================================================================================================================

  :source-paths ["src/demo"
                 "src/debug"]

  :clean-targets ^{:protect false} ["resources/public/_compiled"
                                    "target"]

  :checkout-deps-shares ^:replace []                                                                                          ; http://jakemccrary.com/blog/2015/03/24/advanced-leiningen-checkouts-configuring-what-ends-up-on-your-classpath/

  ; =========================================================================================================================

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {; --------------------------------------------------------------------------------------------------------------
             :demo
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/demo"]
                                    :compiler     {:output-to      "resources/public/_compiled/demo/devtools_sample.js"
                                                   :output-dir     "resources/public/_compiled/demo"
                                                   :asset-path     "_compiled/demo"
                                                   :main           devtools-sample.boot
                                                   :tooling-config {:devtools/config {:features-to-install :all}}
                                                   :preloads       [devtools.preload]
                                                   :optimizations  :none
                                                   :source-map     true}}}}}
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
             {:checkout-deps-shares ^:replace [:source-paths
                                               :test-paths
                                               :resource-paths
                                               :compile-path
                                               #=(eval leiningen.core.classpath/checkout-deps-paths)]
              :cljsbuild            {:builds {:demo
                                              {:source-paths ["checkouts/cljs-devtools/src/lib"]}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :figwheel
             {:figwheel  {:server-port     7000
                          :server-logfile  ".figwheel/server.log"
                          :validate-config false}
              :cljsbuild {:builds {:demo
                                   {:figwheel true}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :devel
             {:env       {:devtools-debug "true"}
              :cljsbuild {:builds {:demo
                                   {:source-paths ["src/debug"
                                                   "checkouts/cljs-devtools/src/debug"]}}}}}

  ; =========================================================================================================================

  :aliases {"demo"                           ["with-profile" "+demo,+figwheel" "figwheel"]
            "demo-advanced"                  ["with-profile" "+demo-advanced" "do"
                                              ["cljsbuild" "once"]
                                              ["shell" "scripts/dev-server.sh"]]
            "cljs"                           ["with-profile" "+demo" "cljsbuild" "auto"]
            "present"                        ["with-profile" "+demo" "do"
                                              ["clean"]
                                              ["cljsbuild" "once"]
                                              ["shell" "scripts/dev-server.sh"]]
            "advanced-unconditional-install" ["with-profile" "+advanced-unconditional-install" "cljsbuild" "once"]
            "advanced-conditional-install"   ["with-profile" "+advanced-conditional-install" "cljsbuild" "once"]
            "advanced-no-install"            ["with-profile" "+advanced-no-install" "cljsbuild" "once"]
            "advanced-compare"               ["do"
                                              ["clean"]
                                              ["advanced-unconditional-install"]
                                              ["advanced-conditional-install"]
                                              ["advanced-no-install"]
                                              ["shell" "scripts/compare-advanced-builds.sh"]]
            "devel"                          ["with-profile" "+demo,+checkouts,+devel,+figwheel" "figwheel"]})
