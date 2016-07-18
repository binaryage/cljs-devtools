(ns devtools.formatters.markup)

(defn markup-method-to-keyword [sym]
  (keyword (second (re-matches #"<(.*)>" (str sym)))))

(defn markup-method? [sym]
  (some? (re-matches #"<.*>" (str sym))))

(defmacro emit-markup-map []
  (let [defs (:defs (:ns &env))
        extract-markup-method (fn [[def-sym def-info]]
                                (if (and (markup-method? def-sym)
                                         (not (:private def-info)))
                                  [(markup-method-to-keyword def-sym) def-sym]))
        markup-methods-mappings (mapcat extract-markup-method defs)]
    `(hash-map ~@markup-methods-mappings)))
