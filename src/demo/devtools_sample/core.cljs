(ns devtools-sample.core
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [clojure.string :as string]
            [devtools-sample.boot :refer [boot!]]
            [devtools.format :as format]))

(boot! "/src/demo/devtools_sample/core.cljs")

; --- MEAT STARTS HERE -->
; note: (log ...) expands to (.log js/console ...)

(defn hello [name]
  (str "hello, " name "!"))

(def showcase-value {:number  0
                     :string  "string"
                     :keyword :keyword
                     :symbol  'symbol
                     :vector  [0 1 2 3 4 5 6]
                     :set     '#{a b c}
                     :map     '{k1 v1 k2 v2}})

(log nil 43 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] {:k1 1 :k2 2} #{1 2 3})                                           ; simple values
(log [nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] {:k1 1 :k2 2} #{1 2 3}])                                         ; vector of simple values
(log (range 100) (range 101) (range 220) (interleave (repeat :even) (repeat :odd)))                                           ; lists, including infinite ones
(log {:k1 'v1 :k2 'v2 :k3 'v3 :k4 'v4 :k5 'v5 :k6 'v6 :k7 'v7 :k8 'v8 :k9 'v9})                                               ; maps
(log #{1 2 3 4 5 6 7 8 9 10 11 12 13 14 15})                                                                                  ; sets
(log hello filter js/document.getElementById)                                                                                 ; functions cljs / native
(log (fn []) (fn [p__gen p123]) #(str %) (js* "function(x) { console.log(x); }"))                                             ; lambda functions
(log [#js {:key "val"} #js [1 2 3] (js-obj "k1" "v1" "k2" :v2) js/window])                                                    ; js interop
(log [1 2 3 [10 20 30 [100 200 300 [1000 2000 3000 :*]]]])                                                                    ; nested vectors
(log (atom showcase-value))                                                                                                   ; atoms
(log (with-meta ["has meta data"] {:some "meta-data"}))                                                                       ; values with metadata

; custom formatter defined in user code
(deftype Person [name address]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white;background-color:#999;padding:0px 4px;" (str "Person: " name)))
  (-has-body [_] (some? address))
  (-body [_] (format/standard-body-template (string/split-lines address))))

(log (Person. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland") (Person. "Mr Homeless" nil))

; <-- MEAT STOPS HERE ---

(def state (atom []))

(defn simple-fn [count]
  (let [rng (range count)]
    (doseq [item rng]
      (let [s (str "item=" item)]
        (swap! state conj s)))))                                                                                              ; <- set breakpoint HERE and see Scope variables in DevTools

(defn fn-returns-nil [])

(defn ^:export sanity-test-handler []
  ((fn-returns-nil) "param"))                                                                                                 ; a test of sanity checker

(defn ^:export breakpoint-test-handler []
  (simple-fn 10)
  (log state))
