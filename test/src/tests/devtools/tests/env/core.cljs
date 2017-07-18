(ns devtools.tests.env.core
  (:require [devtools.protocols :refer [IFormat]]
            [devtools.formatters.templating :refer [make-template]]
            [goog.date]
            [goog.Promise]))

(def REF ["object" {"object" "##REF##"
                    "config" "##CONFIG##"}])

(def CIRCULAR [:string-tag "\"##CIRCULAR##\""])

(def SOMETHING "##SOMETHING##")

(def NATIVE-REF [:native-reference-wrapper-tag :native-reference-background [:native-reference-tag REF]])

(deftype SimpleType [some-field])

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

(deftype TypeIFn4 []
  Fn
  IFn
  (-invoke [this])
  (-invoke [this p1])
  (-invoke [this p--1 p--2 p--3 p--4]))

(def inst-type-ifn0 (TypeIFn0.))
(def inst-type-ifn1 (TypeIFn1.))
(def inst-type-ifn2 (TypeIFn2.))
(def inst-type-ifn4 (TypeIFn4.))

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

(def inst-some-type (SomeType. "xxx"))

; defrecord with IFormat
(defrecord Language [lang]
  IFormat
  (-header [_] (make-template "span" "color:white; background-color:darkgreen; padding: 0px 4px" (str "Language: " lang)))
  (-has-body [_])
  (-body [_]))

(def test-lang (Language. "ClojureScript"))

; reify with IFormat
(def test-reify (reify
                  IFormat
                  (-header [_] (make-template "span"
                                              "color:white; background-color:brown; padding: 0px 4px"
                                              "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))


(defn cljs-fn-with-vec-destructuring [[a b c & rest]])
(defn cljs-fn-with-vec-destructuring-var [& [a b c & rest]])
(defn cljs-fn-with-map-destructuring [{:keys [a b c]}])
(defn cljs-fn-with-map-destructuring-var [& {:keys [a b c]}])

(defn clsj-fn-with-fancy-name#$%!? [arg1! arg? *&mo_re]
  (str "hello!"))

(defn cljs-fn-var [first second & rest])

(defn cljs-fn-multi-arity
  ([a1])
  ([a2_1 a2-2])
  ([a3_1 a3--2 a3--3 a3-4]))

(defn cljs-fn-multi-arity-var
  ([a1])
  ([a2_1 a2-2])
  ([a3_1 a3-2 a3-3 a3-4])
  ([va1 va2 va3 va4 & rest]))

(def cljs-lambda-multi-arity (fn
                               ([] 1)
                               ([a b])
                               ([c d e f])))

(def cljs-lambda-multi-arity-var (fn
                                   ([p1] 1)
                                   ([first & rest])))

(defn get-raw-js-obj-implementing-iprintwithwriter []
  (reify
    IPrintWithWriter
    (-pr-writer [_obj writer _opts]
      (write-all writer "I'm raw-js-type-implementing-iprintwithwriter"))))

(defn get-raw-js-obj-implementing-iformat []
  (reify
    IFormat
    (-header [_] (make-template "span"
                                "color:white; background-color:brown; padding: 0px 4px"
                                "testing reify"))
    (-has-body [_] false)
    (-body [_])))

(extend-protocol IPrintWithWriter
  goog.date.Date
  (-pr-writer [obj writer _opts]
    (write-all writer
               "#gdate "
               42
               [(.getYear obj)
                (.getMonth obj)
                (.getDate obj)]
               #js ["test-array"]
               (js-obj "some-key" "test-js-obj")
               :keyword
               'sym
               #"regex")))

(extend-protocol IFormat
  goog.Promise
  (-header [_] (make-template "span"
                              "color:white; background-color:brown; padding: 0px 4px"
                              "I'm a goog.Promise with devtools.protocols.IFormat"))
  (-has-body [_] false)
  (-body [_]))

(deftype T0 [])
(deftype T1 [fld1])
(deftype T2 [fld1 fld2])
(deftype T3 [fld1 fld2 fld3])
(deftype T4 [fld1 fld2 fld3 fld4])
(deftype T5 [fld1 fld2 fld3 fld4 fld5])
(deftype T6 [fld1 fld2 fld3 fld4 fld5 fld6])

(deftype T1+IPrintWithWriter [fld1]
  IPrintWithWriter
  (-pr-writer [_obj writer _opts]
    (write-all writer "from custom printer")))

(defrecord R0 [])
(defrecord R1 [fld1])
(defrecord R2 [fld1 fld2])
(defrecord R3 [fld1 fld2 fld3])
(defrecord R4 [fld1 fld2 fld3 fld4])
(defrecord R5 [fld1 fld2 fld3 fld4 fld5])
(defrecord R6 [fld1 fld2 fld3 fld4 fld5 fld6])
