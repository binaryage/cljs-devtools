(ns devtools.utils.batteries
  (:require [devtools.format :as f]))

(deftype TypeIFn0 []
  Fn
  IFn
  (-invoke [this]))

(deftype TypeIFn1 []
  Fn
  IFn
  (-invoke [this p1]))

(deftype TypeIFn2 []
  Fn
  IFn
  (-invoke [this p1])
  (-invoke [this p1 p2]))

(deftype TypeIFn2va []
  Fn
  IFn
  (-invoke [this p1 p2 & rest]))

(deftype TypeIFn4va []
  Fn
  IFn
  (-invoke [this])
  (-invoke [this p1])
  (-invoke [this p--1 p--2 p--3 p--4])
  (-invoke [this p--1 p--2 p--3 p--4 & p--rest]))

(def inst-type-ifn0 (TypeIFn0.))
(def inst-type-ifn1 (TypeIFn1.))
(def inst-type-ifn2 (TypeIFn2.))
(def inst-type-ifn2va (TypeIFn2va.))
(def inst-type-ifn4va (TypeIFn4va.))

(deftype ATypeWithIFn []
  Fn
  IFn
  (-invoke [this param1])
  (-invoke [this p1 p2])
  (-invoke [this px---1 px---2 & rest]))

(def simple-cljs-fn-source "function devtools_sample$core$hello(name){
  return [cljs.core.str(\"hello, \"),cljs.core.str(name),cljs.core.str(\"!\")].join('');
}")

(def simple-lambda-fn-source "function (p1, p2, p3){}")
(def simplest-fn-source "function (){}")
(def invalid-fn-source "function [xxx] {}")

(def simplest-fn (js* "function (){}"))
(def minimal-fn (js* "function (){return 1}"))

(defn sample-cljs-fn [p1 p2 & rest]
  (println p1 p2 rest))

(deftype SomeType [some-field])

; defrecord with IDevtoolsFormat
(defrecord Language [lang]
  f/IDevtoolsFormat
  (-header [_] (f/template "span" "color:white; background-color:darkgreen; padding: 0px 4px" (str "Language: " lang)))
  (-has-body [_])
  (-body [_]))

(def test-lang (Language. "ClojureScript"))

; reify with IDevtoolsFormat
(def test-reify (reify
                  f/IDevtoolsFormat
                  (-header [_] (f/template "span"
                                           "color:white; background-color:brown; padding: 0px 4px"
                                           "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))