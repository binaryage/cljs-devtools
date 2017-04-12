(ns devtools.prefs
  (:require-macros [devtools.prefs :refer [emit-external-config emit-env-config]])
  (:require [devtools.defaults :as defaults]))

; we use delay for DCE, see https://github.com/binaryage/cljs-devtools/issues/37
(def default-config (delay @defaults/config))
(def external-config (delay (emit-external-config)))
(def env-config (delay (emit-env-config)))
(def initial-config (delay (merge @default-config @external-config @env-config)))

(def ^:dynamic *current-config* (delay @initial-config))

; -- public api -------------------------------------------------------------------------------------------------------------

(defn set-prefs! [new-prefs]
  (set! *current-config* new-prefs))

(defn get-prefs []
  (when (delay? *current-config*)
    (set-prefs! @*current-config*))
  *current-config*)

(defn pref [key]
  (key (get-prefs)))

(defn set-pref! [key val]
  (set-prefs! (assoc (get-prefs) key val)))

(defn merge-prefs! [m]
  (set-prefs! (merge (get-prefs) m)))

(defn update-pref! [key f & args]
  (let [new-val (apply f (pref key) args)]
    (set-pref! key new-val)))
