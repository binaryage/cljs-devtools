(ns devtools-sample.issue22
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]
            [devtools.protocols :refer [IFormat]]))

(boot! "/src/demo/devtools_sample/issue22.cljs")

(enable-console-print!)

(declare gen-chain)

(defn fold-templates [templates]
  (let [t (first templates)
        t (if (fn? t) (t) t)]
    (if (> (count templates) 1)
      (.concat #js [] t #js [(fold-templates (rest templates))])
      (.concat #js [] t))))

(defn gen-elemental-template [n]
  #js ["span" #js {} n "-"])

(deftype NestedTemplate [n]
  IFormat
  (-header [value]
    (fold-templates (map gen-elemental-template (range n))))
  (-has-body [value] false)
  (-body [value]))

(defn gen-ref [o]
  #js ["object" #js {"object" o}])

(deftype NestedRefTemplate [spec]
  IFormat
  (-header [value]
    (fold-templates (gen-chain spec)))
  (-has-body [value] false)
  (-body [value]))

(defn gen-chain [chain]
  (if-let [head (first chain)]
    (let [ts (map gen-elemental-template (range head))]
      (if-let [tail (seq (rest chain))]
        (concat ts [(gen-ref (NestedRefTemplate. tail))])
        ts))))

(deftype NestedType [ntf1])

(deftype MyType [f1 f2 f3 f4 f5])

(def showcase-value {:number  0
                     :string  "string"
                     :keyword :keyword
                     :symbol  'symbol
                     :vector  [0 1 2 3 4 5 6]
                     :set     '#{a b c}
                     :map     '{k1 v1 k2 v2}})

(defprotocol MyProtocol
  (-my-method [o p1] [o p1 p2]))

(deftype BareType [])

(deftype SimpleType [p1])

; --- MEAT STARTS HERE -->

(log (MyType. {:key1 {:key11 {:key111 "value"}}} (NestedType. ["nested" "vals"]) "string" 42 nil))

(log (SimpleType. #{:A :B :C}))

(log "16:" (NestedTemplate. 16))
(log "17:" (NestedTemplate. 17))
(log "18:" (NestedTemplate. 18))
(log "19:" (NestedTemplate. 19))

(log "[11 1]:" (NestedRefTemplate. [11 1]))
(log "[11 2]:" (NestedRefTemplate. [11 2]))
(log "[11 3]:" (NestedRefTemplate. [11 3]))
(log "[11 4]:" (NestedRefTemplate. [11 4]))
(log "[11 5]:" (NestedRefTemplate. [11 5]))
(log "[11 6]:" (NestedRefTemplate. [11 6]))

(log "[12 1]:" (NestedRefTemplate. [12 1]))
(log "[12 2]:" (NestedRefTemplate. [12 2]))
(log "[12 3]:" (NestedRefTemplate. [12 3]))
(log "[12 4]:" (NestedRefTemplate. [12 4]))
(log "[12 5]:" (NestedRefTemplate. [12 5]))
(log "[12 6]:" (NestedRefTemplate. [12 6]))

(log "[13 1]:" (NestedRefTemplate. [13 1]))
(log "[13 2]:" (NestedRefTemplate. [13 2]))
(log "[13 3]:" (NestedRefTemplate. [13 3]))
(log "[13 4]:" (NestedRefTemplate. [13 4]))
(log "[13 5]:" (NestedRefTemplate. [13 5]))
(log "[13 6]:" (NestedRefTemplate. [13 6]))

(log "[14 1]:" (NestedRefTemplate. [14 1]))
(log "[14 2]:" (NestedRefTemplate. [14 2]))
(log "[14 3]:" (NestedRefTemplate. [14 3]))
(log "[14 4]:" (NestedRefTemplate. [14 4]))
(log "[14 5]:" (NestedRefTemplate. [14 5]))
(log "[14 6]:" (NestedRefTemplate. [14 6]))

(log "[15 1]:" (NestedRefTemplate. [15 1]))
(log "[15 2]:" (NestedRefTemplate. [15 2]))
(log "[15 3]:" (NestedRefTemplate. [15 3]))
(log "[15 4]:" (NestedRefTemplate. [15 4]))
(log "[15 5]:" (NestedRefTemplate. [15 5]))
(log "[15 6]:" (NestedRefTemplate. [15 6]))

(log "[16 1]:" (NestedRefTemplate. [16 1]))
(log "[16 2]:" (NestedRefTemplate. [16 2]))
(log "[16 3]:" (NestedRefTemplate. [16 3]))
(log "[16 4]:" (NestedRefTemplate. [16 4]))
(log "[16 5]:" (NestedRefTemplate. [16 5]))
(log "[16 6]:" (NestedRefTemplate. [16 6]))

(log "[7 2 1]:" (NestedRefTemplate. [7 2 1]))
(log "[7 2 2]:" (NestedRefTemplate. [7 2 2]))
(log "[7 2 3]:" (NestedRefTemplate. [7 2 3]))

(log "[7 3 1]:" (NestedRefTemplate. [7 3 1]))
(log "[7 3 2]:" (NestedRefTemplate. [7 3 2]))
(log "[7 3 3]:" (NestedRefTemplate. [7 3 3]))

(log "[6 3 1]:" (NestedRefTemplate. [6 3 1]))
(log "[6 3 2]:" (NestedRefTemplate. [6 3 2]))
(log "[6 3 3]:" (NestedRefTemplate. [6 3 3]))

(log "[5 3 1]:" (NestedRefTemplate. [5 3 1]))
(log "[5 3 2]:" (NestedRefTemplate. [5 3 2]))
(log "[5 3 3]:" (NestedRefTemplate. [5 3 3]))


(log "[9 2 1]:" (NestedRefTemplate. [9 2 1]))
(log "[9 2 2]:" (NestedRefTemplate. [9 2 2]))
(log "[9 2 3]:" (NestedRefTemplate. [9 2 3]))
(log "[9 2 4]:" (NestedRefTemplate. [9 2 4]))
(log "[9 2 5]:" (NestedRefTemplate. [9 2 5]))

; <-- MEAT STOPS HERE ---
