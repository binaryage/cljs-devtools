(ns devtools.prefs)

(defn read-external-config []
  (if cljs.env/*compiler*
    (or
      (get-in @cljs.env/*compiler* [:options :external-config :devtools/config])                                              ; https://github.com/bhauman/lein-figwheel/commit/80f7306bf5e6bd1330287a6f3cc259ff645d899b
      (get-in @cljs.env/*compiler* [:options :tooling-config :devtools/config]))))                                            ; :tooling-config is deprecated

(defmacro emit-external-config []
  (or (read-external-config) {}))
