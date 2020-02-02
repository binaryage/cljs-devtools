(ns app.core
  (:require [clojure.string :as string]
            [devtools.formatters.markup :refer [<standard-body>]]
            [devtools.formatters.templating :refer [render-markup]]
            [devtools.protocols :refer [IFormat]]))

(defn hello [name]
  (str "hello, " name "!"))

(def showcase-value {:number  0
                     :string  "string"
                     :keyword :keyword
                     :symbol  'symbol
                     :vector  [0 1 2 3 4 5 6]
                     :set     '#{a b c}
                     :map     '{k1 v1 k2 v2}})

(defprotocol MyProtocol
  (-method1 [this])
  (-method2
    [this param1]
    [this param1 param2]))

(deftype MyType [f1 f2]
  MyProtocol
  (-method1 [this])
  (-method2 [this param1])
  (-method2 [this param1 param2]))

; custom formatter defined in user code
(deftype Person [name address]
  IFormat
  (-header [_] (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Person: " name)]))
  (-has-body [_] (some? address))
  (-body [_] (render-markup (<standard-body> (string/split-lines address)))))

(defn demo-devtools! []
  (js/console.log nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] {:k1 1 :k2 2} #{1 2 3})                              ; simple values
  (js/console.log [nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] {:k1 1 :k2 2} #{1 2 3}])                            ; vector of simple values
  (js/console.log (range 100) (range 101) (range 220) (interleave (repeat :even) (repeat :odd)))                              ; lists, including infinite ones
  (js/console.log {:k1 'v1 :k2 'v2 :k3 'v3 :k4 'v4 :k5 'v5 :k6 'v6 :k7 'v7 :k8 'v8 :k9 'v9})                                  ; maps
  (js/console.log #{1 2 3 4 5 6 7 8 9 10 11 12 13 14 15})                                                                     ; sets
  (js/console.log hello filter js/document.getElementById)                                                                    ; functions cljs / native
  (js/console.log (fn []) (fn [p__gen p123]) #(str %) (js* "function(x) { console.log(x); }"))                                ; lambda functions
  (js/console.log [#js {:key "val"} #js [1 2 3] (js-obj "k1" "v1" "k2" :v2) js/window])                                       ; js interop
  (js/console.log [1 2 3 [10 20 30 [100 200 300 [1000 2000 3000 :*]]]])                                                       ; nested vectors
  (js/console.log (with-meta ["has meta data"] {:some "meta-data"}))                                                          ; values with metadata
  (js/console.log (Person. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland") (Person. "Mr Homeless" nil))          ; custom formatting
  (js/console.log (atom showcase-value))                                                                                      ; atoms
  (js/console.log (MyType. 1 2)))

(defn ^:export main []
  (demo-devtools!))
