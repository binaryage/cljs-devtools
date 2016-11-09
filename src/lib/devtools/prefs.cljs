(ns devtools.prefs
  (:require-macros [devtools.prefs :refer [emit-external-config emit-env-config]])
  (:require [devtools.defaults :as defaults]))

; we cannot use cljs.core/merge because that would confuse advanced mode compilation
; if you look at cljs.core/merge you will see that it relies on reduce which relies on protocol checks and
; this is probably too dymamic to be elided (my theory)
(defn simple-merge [base-map & maps]
  (let [rmaps (reverse maps)
        sentinel (js-obj)
        sentinel? #(identical? % sentinel)
        merged-keys (dedupe (sort (apply concat (map keys rmaps))))]
    (loop [result base-map
           todo-keys merged-keys]
      (if (empty? todo-keys)
        result
        (let [key (first todo-keys)
              val (first (remove sentinel? (map #(get % key sentinel) rmaps)))]
          (recur (assoc result key val) (rest todo-keys)))))))

(def default-config defaults/prefs)
(def external-config (emit-external-config))
(def env-config (emit-env-config))
(def initial-config (simple-merge default-config external-config env-config))

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
