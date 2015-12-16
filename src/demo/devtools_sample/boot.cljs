(ns devtools-sample.boot
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [devtools-sample.logging :refer [log]]
                   [devtools-sample.config :refer [debug? figwheel?]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]
            [devtools.core :as devtools]
            [devtools-sample.figwheel :as figwheel]))

(defn extract-meat [re s]
  (let [rex (js/RegExp. re "igm")]
    (.exec rex s)))

(defn trim-newlines [s]
  (let [rex (js/RegExp. "^\n+|\n+$" "g")]
    (.replace s rex "")))

(defn escape-html [text] (string/escape text {\< "&lt;", \> "&gt;", \& "&amp;"}))

(defn get-meat [source-code]
  (trim-newlines (nth (extract-meat (str "-" "->([^]*?); <-") source-code) 1)))

(defn fetch-source-code []
  (go (let [response (<! (http/get "/src/demo/devtools_sample/core.cljs"))
            block (.querySelector js/document "code")]
        (aset block "innerHTML" (escape-html (get-meat (:body response))))
        (.highlightBlock js/hljs block))))

(defn boot! []
  (when (figwheel?)
    (figwheel/start!))
  (when (debug?)
    (log "devtools-sample: enabled debug mode")
    (set! devtools/*monitor-enabled* true)
    (set! devtools/*sanitizer-enabled* false))
  (devtools/set-pref! :install-sanity-hints true)
  (devtools/install!)
  (fetch-source-code))