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

(defn log-info [& args]
  (.apply (.-info js/console) js/console (to-array args)))

(defn display-banner [prefix installed-features known-features]
  (when-not (prefs/pref :dont-display-banner)
    (let [[fmt-str params] (feature-list-display installed-features known-features)]
      (apply log-info (str prefix " " fmt-str) params))))