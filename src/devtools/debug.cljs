(ns devtools.debug
  (:require [goog.debug.FancyWindow :as fw]
            [goog.debug.Logger :as logger]
            [goog.json :as json]))

; we cannot log into console during console rendering
; hence we build a secondary console for our purposes

(def indentation-spacer "  ")
(def logger-name-padding 7)

(def ^:dynamic *indent* 0)
(def ^:dynamic *console* nil)

(defn logger [name]
  (let [len (count name)
        lpad (- logger-name-padding len)
        stuffing #(apply str (repeat % "."))
        padded-name (str (stuffing lpad) name)]
    (logger/getLogger padded-name)))

(defn indentation []
  (apply str (take *indent* (repeat indentation-spacer))))

(defn indent! []
  (set! *indent* (inc *indent*)))

(defn unindent! []
  (set! *indent* (dec *indent*)))

(defn log [logger & message]
  (.info logger (apply str (cons (indentation) message))))

(defn init-logger! []
  (set! *console* (goog.debug.FancyWindow. "devtools"))
  (.setWelcomeMessage *console* "cljs-devtools side console")
  (.init *console*)
  (.setEnabled *console* true)
  (let [formatter (.getFormatter *console*)]
    (set! (.-showAbsoluteTime formatter) false)
    (set! (.-showRelativeTime formatter) false)
    (set! (.-showLoggerName formatter) true)))

(defn js-apply [f target args]
  (.apply f target (into-array args)))

(defn hijack-console!
  "docstring"
  []
  (let [old-log (aget js/console "log")]
    (aset js/console "log" (fn [& args]
                             (.addSeparator *console*)
                             (apply log (cons (logger "console") args))
                             (js-apply old-log js/console args)))))

(defn init! []
  (hijack-console!)
  (init-logger!))

(defn hook-monitor [name hook]
  (fn [value]
    (log (logger name) value)
    (indent!)
    (let [template (hook value)
          template-filter (fn [key value] (if (= key "object") (str "REF -> " (str value)) value))]
      (log (logger name) "=> " (json/serialize template template-filter))
      (unindent!)
      template)
    ))