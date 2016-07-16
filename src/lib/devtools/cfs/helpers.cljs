(ns devtools.cfs.helpers
  (:require [devtools.prefs :as prefs]))

(defn pref [v]
  (if (keyword? v)
    (recur (prefs/pref v))
    v))

; ---------------------------------------------------------------------------------------------------------------------------

(defn abbreviate-long-string [string marker prefix-limit postfix-limit]
  (str
    (apply str (take prefix-limit string))
    marker
    (apply str (take-last postfix-limit string))))
