(ns devtools-sample.tests.types-tutorial
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/types_tutorial.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(defprotocol MyProtocol
  (-my-method [o p1] [o p1 p2]))

(deftype MyType [p1 p2 p3]
  IHash
  (-hash [_o] 0)

  MyProtocol
  (-my-method [o p1])
  (-my-method [o p1 p2]))

(defrecord MyRecord [r1 r2 r3])

(deftype MyMinimalType [])

(log MyType)
(log (MyType. #{:A :B :C} {:k1 1 :k2 ["a" "b" "c"]} "param3"))
(log (MyRecord. "string" [1 2 3] 'symbol))
(log (MyMinimalType.))

; <-- MEAT STOPS HERE ---
