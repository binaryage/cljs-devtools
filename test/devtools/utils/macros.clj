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