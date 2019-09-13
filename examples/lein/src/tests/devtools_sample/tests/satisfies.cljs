(ns devtools-sample.tests.satisfies
  (:import [goog.date Date DateTime UtcDateTime])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/satisfies.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(defprotocol MyProtocol
  (-my-method [o p1] [o p1 p2]))

(deftype MyType [f1]
  IHash
  (-hash [o] (inc (hash o)))

  MyProtocol
  (-my-method [o p1]
    (log "-my-method1" f1 o p1))
  (-my-method [o p1 p2]
    (log "-my-method2" f1 o p1 p2)))

(def instance (MyType. 42))

(log instance)

(log "satisfies?" (satisfies? MyProtocol instance))                                                                           ; => true

; <-- MEAT STOPS HERE ---
