(ns devtools.formatters.hiccup
  (:require [clojure.walk :refer [postwalk prewalk]]
            [cljs.pprint :refer [pprint]]
            [devtools.formatters.helpers :refer [pref]]
            [devtools.formatters.templating :refer [make-template make-surrogate make-reference]]))

; a renderer from hiccup-like data markup to json-ml
;
; [[tag style] child1 child2 ...] -> #js [tag #js {"style" ...} child1 child2 ...]
;

(defn renderer [markup]
  {:pre [(sequential? markup)]}
  (let [tag-info (pref (first markup))
        children (rest markup)]
    (case tag-info
      "surrogate" (apply make-surrogate children)
      "reference" (apply make-reference children)
      (let [[tag style] tag-info]
        (apply make-template tag style (keep pref children))))))

(defn walk [form]
  (if (sequential? form)
    (renderer (map walk form))
    form))

(defn render-json-ml [markup]
  (walk markup))
