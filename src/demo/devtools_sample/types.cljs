(ns devtools-sample.types
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/demo/devtools_sample/types.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

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

(deftype SimpleType [p1 p2 p$?3]
  IHash
  (-hash [_o] 0)

  MyProtocol
  (-my-method [o p1])
  (-my-method [o p1 p2]))

(defrecord SimpleRecord [r1 r2 r--3])
(defrecord BareRecord [])

(defrecord ExpandableRecord [fld1 fld2 fld3 fld4])

(log SimpleType)
(log (SimpleType. 1 #{:A :B :C} {:k1 1 :k2 ["a" "b" "c"]}) (SimpleRecord. "s" [1 2 3] 'x))

(log (atom showcase-value))                                                                                                   ; atoms
(log (atom (with-meta showcase-value "meta data")))                                                                           ; atoms

(log (with-meta (SimpleRecord. 1 2 3) "record meta"))

(log (BareType.))

(log (ExpandableRecord. 1 js/window "xxx" (atom 1)))

(log (BareRecord.))

(log [1 2 3 4 5])

; <-- MEAT STOPS HERE ---
