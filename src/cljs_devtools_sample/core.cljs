(ns cljs-devtools-sample.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]
            [devtools.core :as devtools]
            [devtools.debug :as devtools-debug]
            [devtools.format :as format]))

(repl/connect "http://localhost:9000/repl")
(enable-console-print!)
(devtools-debug/init!)
(set! devtools/*monitor-enabled* true)
(set! devtools/*sanitizer-enabled* false)
(devtools/install!)

(defn extract-meat [re s]
  (let [rex (js/RegExp. re "igm")]
    (.exec rex s)))

(defn trim-newlines [s]
  (let [rex (js/RegExp. "^\n+|\n+$" "g")]
    (.replace s rex "")))

(defn escape-html [text] (string/escape text {\< "&lt;", \> "&gt;", \& "&amp;"}))

(defn get-meat [source-code]
  (trim-newlines (nth (extract-meat (str "-" "->([^]*?); <-") source-code) 1)))

(go (let [response (<! (http/get "/src/cljs_devtools_sample/core.cljs"))]
      (aset (.querySelector js/document "code") "innerHTML" (escape-html (get-meat (:body response))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; some quick and dirty inline tests

; --- MEAT STARTS HERE -->

(defn log [& args] (.apply (aget js/console "log") js/console (into-array args)))

(log nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] #{1 2 3} {:k1 1 :k2 2} #js [1 2 3] #js {"k1" 1 "k2" 2} (js/Date.))
(log [nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] #{1 2 3} {:k1 1 :k2 2} #js [1 2 3] #js {"k1" 1 "k2" 2} (js/Date.)])
(log (range 100) (range 101) (range 220) (interleave (repeat :even) (repeat :odd)))
(log {:k1 'v1 :k2 'v2 :k3 'v3 :k4 'v4 :k5 'v5 :k6 'v6 :k7 'v7 :k8 'v8 :k9 'v9})
(log #{1 2 3 4 5 6 7 8 9 10 11 12 13 14 15})
(log [[js/window] (js-obj "k1" "v1" "k2" :v2) #(.log js/console "hello") (js* "function(x) { console.log(x); }")])
(log [1 2 3 4 5 [10 20 30 40 50 [100 200 300 400 500 [1000 2000 3000 4000 5000 :*]]]])
(log [1 2 3 [10 20 30 [100 200 300 [1000 2000 3000 :*]]]])
(log (atom {:number 0 :string "string" :keyword :keyword :symbol 'symbol :vector [0 1 2 3 4 5 6] :set '#{a b c} :map '{k1 v1 k2 v2}}))

; custom formatter defined in user code
(deftype Person [name address]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white; background-color:blue; padding: 0px 4px" (str "Person: " name)))
  (-has-body [_] (not (nil? address)))
  (-body [_] (format/standard-body-template (string/split-lines address))))

(log (Person. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland") (Person. "Mr Homeless" nil))

; <-- MEAT STOPS HERE ---

(def test-interleaved #js {"js" true "nested" {:js false :nested #js {"js2" true "nested2" {:js2 false}}}})

; defrecord with IDevtoolsFormat
(defrecord Language [lang]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white; background-color:darkgreen; padding: 0px 4px" (str "Language: " lang)))
  (-has-body [_])
  (-body [_]))

(def test-lang (Language. "ClojureScript"))

; reify with IDevtoolsFormat
(def test-reify (reify
                  format/IDevtoolsFormat
                  (-header [_] (format/template "span" "color:white; background-color:brown; padding: 0px 4px" "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))

(def long-string
  "First line
second line
third line is really looooooooooooooooooooooooooooooooooooooooooooooooooooooooong looooooooooooooooooooooooooooooooooooooooooooooooooooooooong looooooooooooooooooooooooooooooooooooooooooooooooooooooooong

last line")

(defn excercise! []
  (log [test-lang test-reify])
  (log (.-nested test-interleaved))
  (log test-interleaved)
  (log [long-string]))

(excercise!)