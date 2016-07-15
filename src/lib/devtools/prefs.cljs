(ns devtools.prefs
  (:require [devtools.defaults :as defaults]))

(def ^:dynamic *prefs* defaults/prefs)

(defn get-prefs []
  *prefs*)

(defn pref [key]
  (key *prefs*))

(defn set-prefs! [new-prefs]
  (set! *prefs* new-prefs))

(defn set-pref! [key val]
  (set-prefs! (assoc (get-prefs) key val)))

(defn merge-prefs! [m]
  (set-prefs! (merge (get-prefs) m)))

(defn update-pref! [key f & args]
  (let [new-val (apply f (pref key) args)]
    (set-pref! key new-val)))
