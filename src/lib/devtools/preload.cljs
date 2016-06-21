(ns devtools.preload
  (:require-macros [devtools.preload :refer [gen-config]])
  (:require [devtools.prefs :as prefs]
            [devtools.core :as core]))

; this namespace is intended to be included in cljs compiler :preloads
; overrides for default configuration can be specified in :tooling-config > :devtools/config

(def config (gen-config))

(defn apply-config! [prefs-overrides]
  (when-not (empty? prefs-overrides)
    (prefs/merge-prefs! prefs-overrides)))

(apply-config! config)

(if-not (prefs/pref :suppress-preload-install)
  (core/install!))
