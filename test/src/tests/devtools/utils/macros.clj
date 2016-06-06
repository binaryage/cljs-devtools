(ns devtools.utils.macros
  (:refer-clojure :exclude [range = > < + str])
  (:require [clojure.walk :refer [postwalk]]))

(defn resolve-prefs [v]
  (map #(if (keyword? %) (list 'devtools.prefs/pref %) %) v))

(defn resolver [fn args]
  (cons fn (resolve-prefs args)))

(defmacro range [& args] (resolver 'cljs.core/range args))
(defmacro = [& args] (resolver 'cljs.core/= args))
(defmacro > [& args] (resolver 'cljs.core/> args))
(defmacro < [& args] (resolver 'cljs.core/< args))
(defmacro + [& args] (resolver 'cljs.core/+ args))
(defmacro str [& args] (resolver 'cljs.core/str args))

(defmacro with-prefs [prefs & body]
  `(let [orig-prefs# (devtools.core/get-prefs)
         new-prefs# (merge orig-prefs# ~prefs)]
     (devtools.core/set-prefs! new-prefs#)
     ~@body
     (devtools.core/set-prefs! orig-prefs#)))