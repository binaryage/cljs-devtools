(ns devtools.formatters.helpers
  (:require [devtools.prefs :as prefs]))

(defn pref [v]
  (if (keyword? v)
    (recur (prefs/pref v))
    v))

; ---------------------------------------------------------------------------------------------------------------------------

(defn abbreviate-long-string [string marker prefix-limit postfix-limit]
  (let [prefix (apply str (take prefix-limit string))
        postfix (apply str (take-last postfix-limit string))]
    (str prefix marker postfix)))
