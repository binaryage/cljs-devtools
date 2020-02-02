(ns devtools.prefs
  (:require [cljs.env]))

; see https://github.com/binaryage/cljs-devtools/blob/master/docs/configuration.md

; -- external config --------------------------------------------------------------------------------------------------------

(defn read-external-config []
  (if cljs.env/*compiler*
    (or
      (get-in @cljs.env/*compiler* [:options :external-config :devtools/config])                                              ; https://github.com/bhauman/lein-figwheel/commit/80f7306bf5e6bd1330287a6f3cc259ff645d899b
      (get-in @cljs.env/*compiler* [:options :tooling-config :devtools/config]))))                                            ; :tooling-config is deprecated

(defmacro emit-external-config []
  `'~(or (read-external-config) {}))

; -- environmental config ---------------------------------------------------------------------------------------------------

(defn get-env-vars []
  (-> {}
      (into (System/getenv))
      (into (System/getProperties))))

; -- macro config api -------------------------------------------------------------------------------------------------------

(defn read-config []
  (read-external-config))

(def memoized-read-config (memoize read-config))

(defn get-pref [key]
  (key (memoized-read-config)))
