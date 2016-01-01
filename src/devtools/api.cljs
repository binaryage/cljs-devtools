(ns devtools.api
  (:require-macros [devtools.util :refer [oget ocall oapply]])
  (:require [goog.object]
            [clojure.string :as string]
            [devtools.figwheel :refer [figwheel-driver-problem? call-figwheel-driver]]
            [devtools.dirac :as dirac]))

; keep in mind that we want to avoid any state at all
; javascript running this code can be reloaded anytime, same with devtools front-end

(def ^:dynamic wrapper-template
  "(js/devtools.api.present_in_dirac_repl {request-id} {code})")

(defn ^:export eval
  "This function gets called by Dirac DevTools when user enters some input into REPL prompt and hits enter.
  We wrap user input with presentation logic and send it down to Figwheel for compilation.
  Figwheel then compiles it down to javascript and sends it back for evaluation.

  Why do we need to wrap user input here?
  Various CLJS REPLs have a habbit of wrapping compiled code in their own wrappers to convert results to strings (or
  serialize them in some way) before sending them back to server. We don't want to receive string value back,
  for cljs-devtools we need to get the original evaluated javascript value. That is why we want to wrap user code here to
  capture results first.

  Actually, this is not the only wrapping taking place. Evaluating CLJS code snippet turns into an onion of wrappers:
  First, we wrap user's input here to present REPL results in Dirac DevTools.
  Second, server-side REPL compilation system usually wraps generated code in pr-str or similar (see the above paragraph).
  Third, we wrap generated javascript in dirac/eval-js again, to execute standard Figwheel evaluation logic"
  [request-id code]
  (let [wrapped-code (-> wrapper-template
                         (string/replace "{request-id}" request-id)
                         (string/replace "{code}" code))]
    (call-figwheel-driver "eval" request-id wrapped-code code)))

(defn present-repl-result
  "Called by our boilerplate when we capture REPL evaluation result."
  [request-id value]
  (dirac/log request-id "result" value))

(defn present-repl-exception [request-id exception]
  "Called by our boilerplate when we capture REPL evaluation exception."
  (dirac/error request-id "exception" exception))

(defn present-in-dirac-repl [request-id value]
  (try
    (js/devtools.api.present_repl_result request-id value)
    value
    (catch :default e
      (js/devtools.api.present_repl_exception request-id e)
      (throw e))))

(defn ^:export eval-js
  "This function gets called by Dirac DevTools when evaluation javascript expression in the debugger context.
  See dirac/eval-js for more info.

  Here we want to mimic Figwheel behaviour, eval the code as is and send report back to Figwheel server."
  [request-id callback-name expression]
  (.log js/console "got for eval" request-id callback-name expression)
  (call-figwheel-driver "eval_js" expression callback-name))

(defn ^:export warm-up-repl-connection
  "This gets called by Dirac DevTools when user switches to REPL prompt first time. We check REPL-server connection and
  request current REPL namespace."
  []
  (if-let [reason (figwheel-driver-problem?)]
    (.warn js/console reason)
    (do
      (call-figwheel-driver "request_ns")
      true)))