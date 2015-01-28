(ns cljs-devtools-sample.core
  (:require [clojure.browser.repl :as repl]
            [devtools.core :as dev]))

(repl/connect "http://localhost:9000/repl")
(enable-console-print!)

(println "cljs-devtools-sample")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
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
(def test-vector [nil 0 "text" [1 2 3 [10 20 30 [100 200 300]]] '#{a b c} {:k1 'v1 :k2 "v2"} #js {"k1" "v1" "k2" :v2} (js* "function(x) { console.log(x); }")])
(def test-long-vector (range 100))

(defn excercise! []
  (.log js/console test-number)
  (.log js/console test-keyword)
  (.log js/console test-symbol)
  (.log js/console test-vector)
  (.log js/console test-long-vector)
  (.log js/console test-atom))

(.log js/console "****** enable ******")

(.log js/console (dev/build-header test-vector))

(dev/support-devtools!)

(excercise!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; disable support

(.log js/console "****** disable ******")

(dev/unsupport-devtools!)

(excercise!)
