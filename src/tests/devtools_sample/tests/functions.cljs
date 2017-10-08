(ns devtools-sample.tests.functions
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools-sample.boot :refer [boot!]]))

(boot! "/src/tests/devtools_sample/tests/functions.cljs")

; --- MEAT STARTS HERE -->
; note: (log ...) expands to (.log js/console ...)

(defn hello [name]
  (str "hello, " name "!"))

(defn multi-arity-with-var-args
  ([a1])
  ([a2--x a2--y])
  ([a3--10 a3--11 a3--12 a3--13])
  ([p1 p2 p3 p4 p5 & rest]))

(deftype ATypeWithIFn []
  Fn
  IFn
  (-invoke [this p1])
  (-invoke [this px---1 px---2]))

(defn fn-with-fancy-name$%!? [arg1! arg2? & more]
  (str "too fancy"))

(log filter)                                                                                                                  ; core function
(log hello)                                                                                                                   ; recognizes cljs functions
(log fn-with-fancy-name$%!?)                                                                                                  ; knows to demunge names
(log multi-arity-with-var-args)                                                                                               ; multi-arity
(log (ATypeWithIFn.))                                                                                                         ; supports IFn protocol
(log (fn []) (fn [p__gen p123]) #(str %) (js* "function(x) { console.log(x); }"))                                             ; lambda functions
(log js/document.getElementById)                                                                                              ; ignores non-cljs functions

; <-- MEAT STOPS HERE ---
