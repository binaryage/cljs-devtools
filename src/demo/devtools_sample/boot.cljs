(ns devtools-sample.boot
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [devtools-sample.logging :refer [log]]
                   [devtools-sample.config :refer [debug?]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]
            [devtools.core] ; TODO: review this later
            [devtools.custom-formatters :as custom-formatters]))

(defn extract-meat [re s]
  (let [rex (js/RegExp. re "igm")]
    (.exec rex s)))

(defn trim-newlines [s]
  (let [rex (js/RegExp. "^\n+|\n+$" "g")]
    (.replace s rex "")))

(defn escape-html [text] (string/escape text {\< "&lt;", \> "&gt;", \& "&amp;"}))

(defn get-meat [source-code]
  (trim-newlines (nth (extract-meat (str "-" "->([^]*?); <-") source-code) 1)))

(defn fetch-source-code [source-path]
  (go
    (let [response (<! (http/get source-path))
          block (.querySelector js/document "code")]
      (aset block "innerHTML" (escape-html (get-meat (:body response))))
      (.highlightBlock js/hljs block))))

(defn boot! [source-path]
  (when (debug?)
    (log "devtools-sample: enabled debug mode")
    (set! custom-formatters/*monitor-enabled* true)
    (set! custom-formatters/*sanitizer-enabled* false)
    #_(js/goog.require "devtools_sample.debug"))
  (enable-console-print!)
  (fetch-source-code source-path))
