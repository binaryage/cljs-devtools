(defproject binaryage/devtools "0.4.0-SNAPSHOT"
  :description "Experimental Chrome devtools support for ClojureScript"
  :url "https://github.com/binaryage/cljs-devtools"
  :license {:name         "MIT License"
            :url          "http://opensource.org/licenses/MIT"
            :distribution :repo}
  :scm {:name "git"
        :url  "https://github.com/binaryage/cljs-devtools"}
  :signing {:gpg-key "DDD8C87F"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :pom-addition [:developers [:developer
                              [:name "Antonin Hildebrand"]
                              [:url "https://github.com/darwin"]
                              [:email "antonin@hildebrand.cz"]
                              [:timezone "+1"]]]

  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.107" :scope "provided"]]

  :clean-targets ["out"]
  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-cljfmt "0.1.7"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild
  {:builds
                  {:dev
                   {:source-paths ["src", "src-debug"]
                    :compiler     {:output-to     "out/dev/cljs_devtools.js"
                                   :output-dir    "out/dev"
                                   :optimizations :none
                                   :source-map    true}}
                   :prod
                   {:source-paths ["src"]
                    :compiler     {:output-to     "out/prod/cljs_devtools.min.js"
                                   :output-dir    "out/prod"
                                   :optimizations :advanced
                                   :source-map    "out/prod/cljs_devtools.min.js.map"}}
                   :test
                   {:source-paths ["src", "test"]
                    :compiler     {:output-to     "out/test/cljs_devtools.test.js"
                                   :output-dir    "out/test"
                                   :main          devtools.runner
                                   :asset-path    "_generated"
                                   :optimizations :none
                                   :pretty-print  true
                                   :source-map    true}}
                   }
   :test-commands {"unit" ["phantomjs" "test/phantom.js" "test/runner.html"]}
   }

  )

