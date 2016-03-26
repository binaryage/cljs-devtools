(ns devtools.util
  (:require [devtools.prefs :as prefs]))

(defn feature-for-display [installed-features feature]
  (let [color (if (some #{feature} installed-features) "color:#0000ff" "color:#ccc")]
    ["%c%s" [color (str feature)]]))

(defn feature-list-display [installed-features known-features]
  (let [labels (map (partial feature-for-display installed-features) known-features)
        * (fn [accum val]
            [(str (first accum) " " (first val))
             (concat (second accum) (second val))])]
    (reduce * (first labels) (rest labels))))