(ns devtools.formatters.markup)

(defn method-to-keyword [sym]
  (keyword (second (re-matches #"<(.*)>" (str sym)))))

(defmacro emit-markup-map []
  (let [defs (:defs (:ns &env))
        extract-keyword-symbol-pair (fn [[def-sym def-info]]
                                      (if (and (= (first (str def-sym)) \<) (not (:private def-info)))
                                        [(method-to-keyword def-sym) def-sym]))
        markup-methods-mappings (mapcat extract-keyword-symbol-pair defs)]
    `(hash-map ~@markup-methods-mappings)))
