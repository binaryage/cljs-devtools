(ns cljs-devtools-sample.core
  (:require [clojure.browser.repl :as repl]
            [devtools.core :as dev]))

(repl/connect "http://localhost:9000/repl")
(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; some quick and dirty inline tests

(def test-atom
  (atom {:number 0
         :string "sample string"
         :keyword :keyword
         :symbol 'symbol
         :vector [0,1,2]
         :set '#{a b c}
         :map '{k1 v1 k2 v2}
         :form '[(defn greet [name] (str "Hello, " name "!"))]
         :lambda #(println %)}))

(def test-number 42)
(def test-keyword :keyword)
(def test-symbol 'symbol)
(def test-vector [nil 0 0.5 "text" [1 2 3 [10 20 30 [100 200 300]]] '#{a b c} {:k1 'v1 :k2 "v2"} #js {"k1" "v1" "k2" :v2} (js* "function(x) { console.log(x); }")])
(def test-long-vector (range 100))
(def test-large-map {:k1 'v1 :k2 'v2 :k3 'v3 :k4 'v4 :k5 'v5 :k6 'v6 :k7 'v7 :k8 'v8 :k9 'v9})
(def test-large-set #{1 2 3 4 5 6 7 8 9 10})
(def test-interleaved #js {"js" true "nested" {:js false :nested #js {"js2" true "nested2" {:js2 false}}}})

(defn excercise! []
  (.log js/console test-number)
  (.log js/console test-keyword)
  (.log js/console test-symbol)
  (.log js/console test-vector)
  (.log js/console test-long-vector)
  (.log js/console test-large-map)
  (.log js/console test-large-set)
  (.log js/console test-interleaved)
  (.log js/console (.-nested test-interleaved))
  (.log js/console test-atom))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; enable support

(.log js/console "****** enable ******")

(dev/support-devtools!)

(excercise!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; disable support

(.log js/console "****** disable ******")

(dev/unsupport-devtools!)

(excercise!)

; ad-hoc debugging

(.log js/console "****** enable ******")

(dev/support-devtools!)

(.log js/console (dev/build-header test-vector))
