(ns devtools.prefs
  (:require-macros [devtools.prefs :refer [emit-external-config emit-env-config]])
  (:require [devtools.defaults :as defaults]))

; we cannot use cljs.core/merge because that would confuse advanced mode compilation
; if you look at cljs.core/merge you will see that it relies on protocol checks and this is too dymamic to be elided
(defn simple-merge [m1 m2]
  (loop [m m1
         ks (keys m2)]
    (if (empty? ks)
      m
      (recur (assoc m (first ks) (get m2 (first ks))) (rest ks)))))

(def default-config defaults/prefs)
(def external-config (emit-external-config))
(def env-config (emit-env-config))
(def initial-config (-> default-config (simple-merge external-config) (simple-merge env-config)))

(def ^:dynamic *prefs* initial-config)

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
