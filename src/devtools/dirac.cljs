(ns devtools.dirac
  (:require-macros [devtools.util :refer [oget ocall oapply]]
                   [devtools.dirac :refer [gen-config]])
  (:require [goog.object]
            [clojure.browser.repl :as brepl]
            [devtools.prefs :refer [pref]]
            [clojure.string :as string]
            [goog.labs.userAgent.browser :as ua]))

(defn ^:dynamic available? []
  (and (ua/isChrome) (ua/isVersionOrHigher 47)))                                                                              ; Chrome 47+

; ===========================================================================================================================

; Dirac is a codename for our DevTools fork
;
; we didn't want to introduce new protocol methods for websocket connection between DevTools front-end and back-end
; so instead we tunnel our messages through console.log calls
;
; when first paramter of the log message mentions our magic word, we treat the call differently:
; 1) "~~$DIRAC-MSG$~~" is for control messages
;                      these are taken outside of message processing and do not affect console model
; 2) "~~$DIRAC-LOG$~~" is for favored version of normal log statements (they will have green cljs-ish background)
;                      we let these bubble through as real log messages but decorate them slightly for our purposes

(defonce ^:dynamic *installed?* false)

(defonce api-version 1)                                                                                                       ; internal API version

(def default-config
  {:dirac-agent-host             "localhost"
   :dirac-agent-port             "8231"
   :dirac-agent-verbose          false
   :dirac-agent-auto-reconnect   true
   :dirac-agent-response-timeout 5000
   :dirac-weasel-verbose         false
   :dirac-weasel-auto-reconnect  false
   :dirac-weasel-pre-eval-delay  100})

(defonce static-config (gen-config))                                                                                          ; this config is comming from environment and system properties

; keep in mind that we want to avoid any state at all
; javascript running this code can be reloaded anytime, same with devtools front-end

; -- tunneling messages to Dirac DevTools -----------------------------------------------------------------------------------

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

(defn group* [collapsed? request-id kind & args]
  (apply console-tunnel (if collapsed? "groupCollapsed" "group") (dirac-log-args request-id kind args)))

(defn group-collapsed [& args]
  (apply group* true args))

(defn group [& args]
  (apply group* false args))

(defn group-end []
  (.groupEnd js/console))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn detect-and-strip [prefix text]
  (let [prefix-len (count prefix)
        s (subs text 0 prefix-len)]
    (if (= s prefix)
      (string/triml (subs text prefix-len)))))

(defn present-java-trace [request-id text]
  (let [lines (string/split text #"\n")
        first-line (first lines)
        rest-content (string/join "\n" (rest lines))]
    (if (empty? rest-content)
      (error request-id :stderr first-line)
      (do
        (group-collapsed request-id :stderr "%c%s" (pref :java-trace-header-style) first-line)
        (log request-id :stderr rest-content)
        (group-end)))))

(defn build-effective-config [default-config static-config]
  (let [static-keys (keys default-config)
        * (fn [key]
            (if-let [val (pref key)]
              [key val]))
        dynamic-config (into {} (map * static-keys))]
    (merge default-config static-config dynamic-config)))

; -- public API -------------------------------------------------------------------------------------------------------------

(defn ^:export get-effective-config []
  (clj->js (build-effective-config default-config static-config)))

(defn ^:export get-api-version []
  api-version)

(defn ^:export present-repl-result
  "Called by our nREPL boilerplate when we capture REPL evaluation result."
  [request-id value]
  (log request-id "result" value)
  value)

(defn ^:export present-repl-exception
  "Called by our nREPL boilerplate when we capture REPL evaluation exception."
  [request-id exception]
  (error request-id "exception" exception))

(defn ^:export present-in-dirac-repl [request-id value]
  (try
    (js/devtools.dirac.present_repl_result request-id value)
    (catch :default e
      (js/devtools.dirac.present_repl_exception request-id e)
      (throw e))))

(defn ^:export present-output [request-id kind text]
  (case kind
    "java-trace" (present-java-trace request-id text)
    (if-let [warning-msg (detect-and-strip "WARNING:" text)]
      (warn request-id "warning" warning-msg)
      (if-let [error-msg (detect-and-strip "ERROR:" text)]
        (error request-id "error" error-msg)
        (log request-id kind text)))))

(defn ^:export postprocess-successful-eval
  "This is a postprocessing function wrapping weasel javascript evaluation attempt.
  This structure is needed for building response to nREPL server (see dirac.implant.weasel in Dirac project)
  In our case weasel is running in the context of Dirac DevTools and could potentially have different version of cljs runtime.
  To be correct we have to do this post-processing in app's context to use the same cljs runtime as app evaluating the code.

  Also we have to be careful to not enter into infinite printing with cyclic data structures.
  We limit printing level and length."
  [value]
  (binding [cljs.core/*print-level* (pref :dirac-print-level)
            cljs.core/*print-length* (pref :dirac-print-length)]
    #js {:status "success"
         :value  (str value)}))

(defn ^:export postprocess-unsuccessful-eval [e]
  "Same as postprocess-successful-eval but prepares response for evaluation attempt with exception."
  #js {:status     "exception"
       :value      (pr-str e)
       :stacktrace (if (.hasOwnProperty e "stack")
                     (.-stack e)
                     "No stacktrace available.")})

; -- install/uninstall ------------------------------------------------------------------------------------------------------

(defn ^:export install! []
  (when (and (not *installed?*) (available?))
    (set! *installed?* true)
    (brepl/bootstrap)
    true))

(defn ^:export uninstall! []
  (when *installed?*
    (set! *installed?* false)))