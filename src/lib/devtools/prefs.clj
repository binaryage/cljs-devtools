(ns devtools.prefs
  (:require [env-config.core :as env-config]
            [cljs.env]))

; see https://github.com/binaryage/cljs-devtools/blob/master/docs/configuration.md

; -- external config --------------------------------------------------------------------------------------------------------

(defn read-external-config []
  (if cljs.env/*compiler*
    (or
      (get-in @cljs.env/*compiler* [:options :external-config :devtools/config])                                              ; https://github.com/bhauman/lein-figwheel/commit/80f7306bf5e6bd1330287a6f3cc259ff645d899b
      (get-in @cljs.env/*compiler* [:options :tooling-config :devtools/config]))))                                            ; :tooling-config is deprecated

(defmacro emit-external-config []
  (or (read-external-config) {}))

; -- environmental config ---------------------------------------------------------------------------------------------------

(def ^:dynamic env-config-prefix "cljs-devtools")

(defn get-env-vars []
  (-> {}
      (into (System/getenv))
      (into (System/getProperties))))

(defn read-env-config []
  (env-config/make-config-with-logging env-config-prefix (get-env-vars)))

(def memoized-read-env-config (memoize read-env-config))

(defmacro emit-env-config []
  (or (memoized-read-env-config) {}))
