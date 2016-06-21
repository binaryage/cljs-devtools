(ns devtools.preload)

(defn read-config []
  (if cljs.env/*compiler*
    (get-in @cljs.env/*compiler* [:options :tooling-config :devtools/config])))

(defmacro gen-config []
  (or (read-config) {}))
