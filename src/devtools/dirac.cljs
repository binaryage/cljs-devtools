(ns devtools.dirac
  (:require-macros [devtools.util :refer [oget ocall oapply]])
  (:require [goog.object]
            [devtools.figwheel :as figwheel]))

; Dirac is a codename for our DevTools fork
; we didn't want to introduce a new protocol methods for websocket connection between DevTools front-end and back-end
; so instead we tunnel our messages through console.log calls
;
; when first paramter of the log message mentions our magic word, we treat the call differently:
; 1) "~~$DIRAC-MSG$~~" is for control messages
;                      these are taken outside of message processing and do not affect console model
; 2) "~~$DIRAC-REPL-RESULT$~~" is for favored version of normal log statements
;                              we let these bubble through as real log messages but decorate them slightly for our purposes

(defonce ^:dynamic *installed?* false)

(defn console-log [& args]
  (.apply (.-log js/console) js/console (apply array args)))

(defn log-warning [& args]
  (.apply (.-warn js/console) js/console (apply array args)))

(defn call-dirac [name & args]
  (apply console-log (concat ["~~$DIRAC-MSG$~~" name] args)))

(defn log-repl-result [request-id & args]
  (apply console-log (concat ["~~$DIRAC-REPL-RESULT$~~" request-id] args)))

(defn process-message [msg]
  (case (:command msg)
    :job-start true
    :job-end true
    :repl-ns (call-dirac "update-current-ns" (:ns msg))
    :output (log-repl-result (:request-id msg) (:content msg))
    (log-warning "Received unrecognized message " (:command msg) " from Figwheel" msg)))

(defn install! []
  (when-not *installed?*
    (set! *installed?* true)
    (figwheel/subscribe! process-message)))

(defn uninstall! []
  (when *installed?*
    (set! *installed?* false)
    (figwheel/unsubscribe! process-message)))