(ns devtools-sample.tests.toolbox
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]
            [devtools.toolbox :as toolbox]))

(boot! "/src/tests/devtools_sample/tests/toolbox.cljs")

; --- MEAT STARTS HERE -->
; note: (log ...) expands to (.log js/console ...)

; -- toolbox/envelope -------------------------------------------------------------------------------------------------------

(info "toolbox/envelope tests:")

(def busy-obj (js-obj "a" 1 "s" "string" "f" (fn some-fn [] (str "do" "something"))))

(def e toolbox/envelope)

(log (e (range 20)))
(log (e js/window "win envelope with a custom header"))
(log (e "hello!" #(str "envelope with a custom header function, the wrapped value is '" % "'")))
(log (e busy-obj
        "busy js-object envelope with a custom style"
        "color:white;background-color:purple;padding:0px 4px;"
        "div"))

; -- toolbox/force-format ---------------------------------------------------------------------------------------------------

(info "toolbox/force-format tests:")

(def ff toolbox/force-format)

(log "string")
(log (ff "string"))
(log 100)
(log (ff 100))
(log 1.1)
(log (ff 1.1))
(log nil)
(log (ff nil))
(log #"regexp")
(log (ff #"regexp"))
(log (js/Date.))
(log (ff (js/Date.)))

; <-- MEAT STOPS HERE ---
