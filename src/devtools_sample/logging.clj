(ns devtools-sample.logging)

(defmacro log [& args]
  `(.log js/console ~@args))
