(ns devtools.dirac
  (:require-macros [devtools.util :refer [oget ocall oapply]])
  (:require [goog.object]
            [devtools.figwheel :as figwheel]
            [clojure.string :as string]))

; Dirac is a codename for our DevTools fork
;
; we didn't want to introduce new protocol methods for websocket connection between DevTools front-end and back-end
; so instead we tunnel our messages through console.log calls
;
; when first paramter of the log message mentions our magic word, we treat the call differently:
; 1) "~~$DIRAC-MSG$~~" is for control messages
;                      these are taken outside of message processing and do not affect console model
; 2) "~~$DIRAC-LOG$~~" is for favored version of normal log statements
;                      we let these bubble through as real log messages but decorate them slightly for our purposes

(defonce ^:dynamic *installed?* false)

(defn console-tunnel [method & args]
  (.apply (oget js/console method) js/console (apply array args)))

(defn dirac-msg-args [name args]
  (concat ["~~$DIRAC-MSG$~~" name] args))

(defn dirac-log-args [request-id kind args]
  (concat ["~~$DIRAC-LOG$~~" request-id kind] args))

(defn call-dirac [name & args]
  (apply console-tunnel "log" (dirac-msg-args name args)))

(defn log [request-id kind & args]
  (apply console-tunnel "log" (dirac-log-args request-id kind args)))

(defn warn [request-id kind & args]
  (apply console-tunnel "warn" (dirac-log-args request-id kind args)))

(defn error [request-id kind & args]
  (apply console-tunnel "error" (dirac-log-args request-id kind args)))

(defn detect-and-strip [prefix text]
  (let [prefix-len (count prefix)
        s (subs text 0 prefix-len)]
    (if (= s prefix)
      (string/triml (subs text prefix-len)))))

(defn present-output [request-id kind text]
  (if-let [warning-msg (detect-and-strip "WARNING:" text)]
    (warn request-id "warning" warning-msg)
    (if-let [error-msg (detect-and-strip "ERROR:" text)]
      (error request-id "error" error-msg)
      (log request-id kind text))))

(defn process-message [msg]
  (case (:command msg)
    :job-start (call-dirac "job-start" (:request-id msg))
    :job-end (call-dirac "job-end" (:request-id msg))
    :repl-ns (call-dirac "repl-ns" (:ns msg))
    :output (present-output (:request-id msg) (name (:kind msg)) (:content msg))
    (.warn js/console (str "Received unrecognized message '" (:command msg) "' from Figwheel") msg)))

(defn install! []
  (when-not *installed?*
    (set! *installed?* true)
    (figwheel/subscribe! process-message)))

(defn uninstall! []
  (when *installed?*
    (set! *installed?* false)
    (figwheel/unsubscribe! process-message)))