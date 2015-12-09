(ns devtools-sample.dev.repl
  (:require [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------

(defonce ^:const repl-marker-style "color:white; background-color:black; padding:0px 2px; border-radius:1px;")
(defonce ^:const figwheel-pattern #"^\(function \(\)\{try\{return cljs\.core\.pr_str\.call")
(defonce ^:const figwheel-replacement "(function (){try{return cljs.core.identity.call")
(defonce ^:const intellij-pattern #"^try\{cljs\.core\.pr_str\.call")
(defonce ^:const intellij-replacement "try{cljs.core.identity.call")

(defonce ^:dynamic *inside-repl-plugin* false)

; --------------------------------------------------------------------------------------------------------------------

(defn should-be-ignored? [code]
  (boolean (.match code #"^goog\.(addDependency|require|provide)")))                                                  ; for some reason we are getting goog.* calls from figwheel inside repl plugin

(defn detect-repl-kind [code]
  (cond
    (.match code figwheel-pattern) :figwheel
    (.match code intellij-pattern) :intellij
    :else :unknown))

(defn unwrap-code [repl-kind code]
  (case repl-kind
    :figwheel (.replace code figwheel-pattern figwheel-replacement)
    :intellij (.replace code intellij-pattern intellij-replacement)
    code))

(defn wrap-result [repl-kind result]
  (case repl-kind
    :figwheel (pr-str result)
    :intellij (pr-str result)
    result))

(defn eval [context code]
  (js* "eval(~{code})"))

(defn echo-result [result]
  (.log js/console "%cREPL" repl-marker-style result))

(defn eval-with-echoing [context code]
  (let [repl-kind (detect-repl-kind code)
        rewritten-code (unwrap-code repl-kind code)
        result (eval context rewritten-code)]
    (echo-result result)
    (wrap-result repl-kind result)))

(defn echoing-eval [context code]
  (if (and *inside-repl-plugin* (not (should-be-ignored? code)))
    (eval-with-echoing context code)
    (eval context code)))

(defn repl-plugin [& args]
  (let [standard-impl (apply figwheel/repl-plugin args)]
    (fn [& args]
      (binding [*inside-repl-plugin* true]
        (apply standard-impl args)))))