(ns devtools.util
  (:require [devtools.prefs :as prefs]))

(defn feature-for-display [known-features feature]
  (let [feature-installation-key (feature known-features)
        enabled? (prefs/pref feature-installation-key)
        color (if enabled? "color:#0000ff" "color:#aaaaaa")]
    ["%c%s" [color (name feature)]]))

(defn feature-list-display [known-features]
  (let [features (keys known-features)
        labels (map (partial feature-for-display known-features) features)
        * (fn [accum val]
            [(str (first accum) " " (first val))
             (concat (second accum) (second val))])]
    (reduce * (first labels) (rest labels))))

(defn log-info [& args]
  (.apply (.-log js/console) js/console (to-array args)))

(defn display-banner [prefix known-features]
  (when-not (prefs/pref :dont-display-banner)
    (let [[fmt-str params] (feature-list-display known-features)]
      (apply log-info (str prefix " " fmt-str) params))))