(ns devtools-sample.issue23
  (:refer-clojure :exclude [ExceptionInfo ->ExceptionInfo ExceptionInfoTypeTemplate pr-writer-ex-info])
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]
            [devtools.protocols :refer [IFormat]]
            [goog.object :as gobject]))

(boot! "/src/demo/devtools_sample/issue23.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

; see http://dev.clojure.org/jira/browse/CLJS-1722

(defn- pr-writer-ex-info [obj writer opts]
  (-write writer "#error {:message ")
  (pr-writer (.-message obj) writer opts)
  (when (.-data obj)
    (-write writer ", :data ")
    (pr-writer (.-data obj) writer opts))
  (when (.-cause obj)
    (-write writer ", :cause ")
    (pr-writer (.-cause obj) writer opts))
  (-write writer "}"))

(deftype ExceptionInfo [message data cause]
  Object
  (toString [this] (pr-str* this))

  IPrintWithWriter
  (-pr-writer [obj writer opts]
    (pr-writer-ex-info obj writer opts)))

(def ExceptionInfoTypeTemplate ExceptionInfo)

(defn ^{:jsdoc ["@constructor"]}
ExceptionInfo [message data cause]
  (let [e (js/Error. message)]
    (this-as this
      (.call ExceptionInfoTypeTemplate this message data cause)
      (do
        (set! (.-name this) (.-name e))
        ;; non-standard
        (set! (.-description this) (.-description e))
        (set! (.-number this) (.-number e))
        (set! (.-fileName this) (.-fileName e))
        (set! (.-lineNumber this) (.-lineNumber e))
        (set! (.-columnNumber this) (.-columnNumber e))
        (set! (.-stack this) (.-stack e)))
      this)))

(gobject/extend ExceptionInfo ExceptionInfoTypeTemplate)
(set! (.. ExceptionInfo -prototype) ExceptionInfoTypeTemplate.prototype)
(set! (.. ExceptionInfo -prototype -constructor) ExceptionInfo)
(set! (.. ExceptionInfo -prototype -__proto__) js/Error.prototype)

;(throw (ExceptionInfo. "I'm ex-info X" {:with "some data"} "cause"))
(log (ExceptionInfo. "I'm ex-info X" {:with "some data"} "cause"))
;(log (pr-str (ExceptionInfo. "I'm ex-info Y" {:with "some data"} "cause")))
;(def a (ExceptionInfo. "I'm ex-info Z" {:with "some data"} "cause"))
;(throw (ExceptionInfo. "I'm ex-info A" {:with "some data"} "cause"))

; ----------

(log (cljs.core/ex-info "I'm logged ex-info from CLJS" {:with "some data"} "and no real cause"))
;(throw (cljs.core/ex-info "I'm thrown ex-info from CLJS" {:with "some data"} "and no real cause"))

; <-- MEAT STOPS HERE ---
