(ns devtools-sample.tests.issue52
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]
            [goog.dom :as gdom]
            [goog.string :as gstr]))

(boot! "/src/tests/devtools_sample/tests/issue52.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->
(def v {:a (fn [])
        :b (fn [p__gen p123])
        :c #(str %)
        :d (js* "function(x) { console.log(x); }")})

(js/console.log v)

(defn trigger-me []
  (let [v1 v])
  (js-debugger))

(def button-node
  (gdom/constHtmlToNode (.from gstr/Const "<button onclick='devtools_sample.tests.issue52.trigger_me()'>Trigger me!</button>")))

(gdom/insertChildAt js/document.body button-node 0)

; <-- MEAT STOPS HERE ---
