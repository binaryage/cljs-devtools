(ns devtools.testenv
  (:require-macros [devtools.runner :refer [emit-clojure-version]])
  (:require [devtools.context :as context]))

; this namespace gets included prior devtools.runner

(enable-console-print!)

(println "Clojure version:" (emit-clojure-version))
(println "ClojureScript version:" *clojurescript-version*)

(def silent-console
  #js {
       "info"  (fn [& _args])                                                                                                 ; silent
       "log"   (fn [& _args])                                                                                                 ; silent
       "warn"  (fn [& _args])                                                                                                 ; silent
       "error" (fn [& args]
                 (.apply js/console.error js/console (into-array args)))})

(set! context/get-console (fn [] silent-console))
