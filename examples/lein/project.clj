(def figwheel-version "0.5.19")
(defproject binaryage/devtools-sample "0.1.0-SNAPSHOT"
  :description "An example integration of cljs-devtools"
  :url "https://github.com/binaryage/cljs-devtools"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "0.7.559"]
                 [binaryage/devtools "1.0.0"]
                 [binaryage/dirac "RELEASE"]
                 [com.cognitect/transit-clj "0.8.319"]
                 [cljs-http "0.1.46"]
                 [figwheel ~figwheel-version]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel ~figwheel-version]
            [lein-shell "0.5.0"]]

  ; =========================================================================================================================

  :source-paths ["src/demo"
                 "src/debug"
                 "src/tests"
                 "src/advanced-unconditional-install"
                 "src/advanced-conditional-install"
                 "src/advanced-no-install"]

  :clean-targets ^{:protect false} ["resources/public/.compiled"
                                    "target"]

  :checkout-deps-shares ^:replace []                                                                                          ; http://jakemccrary.com/blog/2015/03/24/advanced-leiningen-checkouts-configuring-what-ends-up-on-your-classpath/

  ; =========================================================================================================================

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {; --------------------------------------------------------------------------------------------------------------
             :demo
             {:cljsbuild {:builds {:demo
                                   {:source-paths ["src/demo"
                                                   "src/tests"]
                                    :compiler     {:output-to     "resources/public/.compiled/demo/devtools_sample.js"
                                                   :output-dir    "resources/public/.compiled/demo"
                                                   :asset-path    ".compiled/demo"
                                                   :main          devtools-sample.boot
                                                   :preloads      [devtools.preload]
                                                   :optimizations :none
                                                   :source-map    true}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :demo-advanced
             {:cljsbuild {:builds {:demo-advanced
                                   {:source-paths ["src/demo"
                                                   "src/tests"]
                                    :compiler     {:output-to     "resources/public/.compiled/demo_advanced/devtools_sample.js"
                                                   :output-dir    "resources/public/.compiled/demo_advanced"
                                                   :asset-path    ".compiled/demo_advanced"
                                                   :pseudo-names  true
                                                   :optimizations :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-unconditional-install
             {:cljsbuild {:builds {:advanced-unconditional-install
                                   {:source-paths ["src/advanced-unconditional-install"]
                                    :compiler     {:output-to       "resources/public/.compiled/advanced-unconditional-install/devtools_sample.js"
                                                   :output-dir      "resources/public/.compiled/advanced-unconditional-install"
                                                   :asset-path      ".compiled/advanced-unconditional-install"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-conditional-install
             {:cljsbuild {:builds {:advanced-conditional-install
                                   {:source-paths ["src/advanced-conditional-install"]
                                    :compiler     {:output-to       "resources/public/.compiled/advanced-conditional-install/devtools_sample.js"
                                                   :output-dir      "resources/public/.compiled/advanced-conditional-install"
                                                   :asset-path      ".compiled/advanced-conditional-install"
                                                   :closure-defines {"goog.DEBUG" false}
                                                   :pseudo-names    true
                                                   :optimizations   :advanced}}}}}
             ; --------------------------------------------------------------------------------------------------------------
             :advanced-no-install
             {:cljsbuild {:builds {:advanced-no-install
                                   {:source-paths ["src/advanced-no-install"]
                                    :compiler     {:output-to       "resources/public/.compiled/advanced-no-install/devtools_sample.js"
                                                   :output-dir      "resources/public/.compiled/advanced-no-install"
                                                   :asset-path      ".compiled/advanced-no-install"
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
             {:cljsbuild {:builds {:demo
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
            "devel"                          ["shell" "scripts/devel.sh"]})
