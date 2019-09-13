(ns devtools-sample.config)

(defn- read-system-env []
  (->> (System/getenv)
       (into {})))

(def env (read-system-env))

(defmacro debug? []
  (boolean (env "DEVTOOLS_DEBUG")))
