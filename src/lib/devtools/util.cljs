(ns devtools.util
  (:require [goog.userAgent :as ua]
            [devtools.version :refer [get-current-version]]
            [devtools.prefs :as prefs]))

(defn ^:dynamic make-version-info []
  (let [version (get-current-version)]
    (str "v" version)))

(defn ^:dynamic make-lib-info []
  (str "CLJS DevTools " (get-current-version)))

(defn ^:dynamic unknown-feature-msg [feature known-features lib-info]
  (str "No such feature " feature " is currently available in " lib-info ". "
       "The list of supported features is " (pr-str known-features)))

(defn ^:dynamic feature-not-available-msg [feature]
  (str "Feature " feature " cannot be installed. "
       "Unsupported browser " (ua/getUserAgentString) "."))

(defn get-lib-info []
  (make-lib-info))

; -- banner -----------------------------------------------------------------------------------------------------------------

(defn feature-for-display [installed-features feature]
  (let [color (if (some #{feature} installed-features) "color:#0000ff" "color:#ccc")]
    ["%c%s" [color (str feature)]]))

(defn feature-list-display [installed-features feature-groups]
  (let [labels (map (partial feature-for-display installed-features) (:all feature-groups))
        * (fn [accum val]
            [(str (first accum) " " (first val))
             (concat (second accum) (second val))])]
    (reduce * (first labels) (rest labels))))

(defn display-banner! [installed-features feature-groups fmt & params]
  (let [[fmt-str fmt-params] (feature-list-display installed-features feature-groups)
        items (concat [(str fmt " " fmt-str)] params fmt-params)]
    (.apply (.-info js/console) js/console (into-array items))))

(defn display-banner-if-needed! [features-to-install feature-groups]
  (when-not (prefs/pref :dont-display-banner)
    (let [banner (str "Installing %c%s%c and enabling features")
          lib-info-style "color:black;font-weight:bold;"
          reset-style "color:black"]
      (display-banner! features-to-install feature-groups banner lib-info-style (get-lib-info) reset-style))))

; -- unknown features -------------------------------------------------------------------------------------------------------

(defn report-unknown-features! [features known-features]
  (let [lib-info (get-lib-info)]
    (doseq [feature features]
      (if-not (some #{feature} known-features)
        (.warn js/console (unknown-feature-msg feature known-features lib-info))))))

(defn is-known-feature? [known-features feature]
  (boolean (some #{feature} known-features)))

(defn sanititze-features! [features feature-groups]
  (let [known-features (:all feature-groups)]
    (report-unknown-features! features known-features)
    (filter (partial is-known-feature? known-features) features)))

(defn resolve-features! [features-desc feature-groups]
  (let [features (cond
                   (and (keyword? features-desc) (features-desc feature-groups)) (features-desc feature-groups)
                   (nil? features-desc) (:default feature-groups)
                   (seqable? features-desc) features-desc
                   :else [features-desc])]
    (sanititze-features! features feature-groups)))

; -- installer --------------------------------------------------------------------------------------------------------------

(defn install-feature! [feature features-to-install available-fn install-fn]
  (if (some #{feature} features-to-install)
    (if (or (prefs/pref :bypass-availability-checks) (available-fn feature))
      (install-fn)
      (.warn js/console (feature-not-available-msg feature)))))