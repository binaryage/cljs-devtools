(ns devtools.debug
  (:require [goog.debug.FancyWindow]
            [goog.debug.Logger :as logger]
            [goog.json :as json]))

; we cannot log into console during console rendering
; hence we build a secondary console for our purposes

(def indentation-spacer "  ")
(def logger-name-stuffer "_")
(def logger-name-padding 8)
(def additional-console-styles ".logmsg, .logmsg * {font-size:10px;}")

(def ^:dynamic *initialized* false)
(def ^:dynamic *indent* 0)
(def ^:dynamic *console* nil)

(def ^:dynamic *debug-print-length* 10)
(def ^:dynamic *debug-print-level* 5)

(defn indentation []
  (apply str (take *indent* (repeat indentation-spacer))))

(defn indent! []
  (set! *indent* (inc *indent*)))

(defn unindent! []
  (set! *indent* (dec *indent*)))

(defn logger [name]
  (let [len (count name)
        lpad (- logger-name-padding len)
        stuffing #(apply str (repeat % logger-name-stuffer))
        padded-name (str (stuffing lpad) name)]
    (logger/getLogger padded-name)))

(defn log [logger message]
  (binding [*print-length* *debug-print-length*
            *print-level* *debug-print-level*]
    (.info logger (apply str (cons (indentation) (str message))))))

(defn ^:export log-exception [message]
  (binding [*print-length* *debug-print-length*
            *print-level* *debug-print-level*]
    (.shout (logger "ex!") (apply str (cons (indentation) (str message))))))

(defn ^:export log-info [message]
  (binding [*print-length* *debug-print-length*
            *print-level* *debug-print-level*]
    (.info (logger "info") (apply str (cons (indentation) (str message))))))

(defn ^:export monitor-api-call [name api-call args]
  (try
    (log (logger name) (str args))                          ; potential exception converting args to string
    (catch :default e
      (log-exception (str e))))
  (indent!)
  (let [api-response (apply api-call args)
        api-response-filter (fn [key value] (if (= key "object") "##REF##" value))]
    (log (logger name) (str "=> " (js->clj (json/parse (json/serialize api-response api-response-filter)))))
    (unindent!)
    api-response))

(defn init-logger! []
  (set! *console* (goog.debug.FancyWindow. "devtools"))
  (aset *console* "getStyleRules" #(str (goog.debug.FancyWindow.prototype.getStyleRules) additional-console-styles))
  (.setWelcomeMessage *console* "cljs-devtools auxiliary console")
  (.init *console*)
  (.setEnabled *console* true)
  (let [formatter (.getFormatter *console*)]
    (set! (.-showAbsoluteTime formatter) false)
    (set! (.-showRelativeTime formatter) false)
    (set! (.-showLoggerName formatter) true)))

(defn hijack-console! []
  (let [original-log-fn (aget js/console "log")]
    (aset js/console "log" (fn [& args]
                             (.addSeparator *console*)
                             (try
                               (log (logger "console") (str args)) ; potential exception converting args to string
                               (catch :default e
                                 (log-exception (str e))))
                             (.apply original-log-fn js/console (into-array args))))))

(defn init! []
  (if *initialized*
    (.warn js/console "devtools.debug already initialized, nothing to do")
    (do
      (init-logger!)
      (hijack-console!)
      (set! *initialized* true))))