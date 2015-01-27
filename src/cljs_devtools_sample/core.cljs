(ns cljs-devtools-sample.core
  (:require [clojure.browser.repl :as repl]
            [devtools.core :as dev]))

(repl/connect "http://localhost:9000/repl")
(enable-console-print!)

(println "cljs-devtools-sample")

(.log js/console "****** enable ******")

(dev/support-devtools!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; some quick and dirty inline tests

(def test-atom
  (atom {:number 0
         :string "sample string"
         :keyword :key
         :symbol 'sym
         :set '#{a b c}
         :map '{k1 v1 k2 v2}
         :form '[(defn greet [name] (str "Hello, " name "!"))]
         :lambda #(println %)}))

(def test-number 42)
(def test-vector [0,1,2,3,:end])

(.log js/console test-number)
(.log js/console test-vector)
(.log js/console test-atom)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; disable support

(.log js/console "****** disable ******")

(dev/unsupport-devtools!)

(.log js/console test-number)
(.log js/console test-vector)
(.log js/console test-atom)

