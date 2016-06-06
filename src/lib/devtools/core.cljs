(ns devtools.core
  (:require [devtools.version :refer [get-current-version]]
            [devtools.prefs :as prefs]
            [devtools.sanity-hints :as sanity-hints]
            [devtools.custom-formatters :as custom-formatters]
            [devtools.util :refer-macros [display-banner]]
            [goog.userAgent :as ua]))

(def known-features [:custom-formatters :sanity-hints])
(def features-to-install-by-default [:custom-formatters])

(defn ^:dynamic make-version-info []
  (let [version (get-current-version)]
    (str "v" version)))

(defn ^:dynamic make-lib-info []
  (str "CLJS DevTools " (make-version-info)))

(defn ^:dynamic missing-feature-warning [feature known-features]
  (str "No such feature " feature " is currently available in " (make-lib-info) ". "
       "The list of supported features is " (pr-str known-features)))

(defn ^:dynamic warn-feature-not-available [feature]
  (.warn js/console (str "Feature " feature " cannot be installed. Unsupported browser " (ua/getUserAgentString) ".")))

; -- public API -------------------------------------------------------------------------------------------------------------

(defn set-prefs! [new-prefs]
  (prefs/set-prefs! new-prefs))

(defn get-prefs []
  (prefs/get-prefs))

(defn set-pref! [pref val]
  (prefs/set-pref! pref val))

(defn is-feature-available? [feature]
  (case feature
    :custom-formatters (custom-formatters/available?)
    :sanity-hints (sanity-hints/available?)))

(defn install!
  ([] (install! features-to-install-by-default))
  ([features-to-install]
   (let [banner (str "Installing %c%s%c and enabling features")
         lib-info (make-lib-info)
         lib-info-style "color:black;font-weight:bold;"
         reset-style "color:black"]
     (display-banner features-to-install known-features banner lib-info-style lib-info reset-style)
     (if (some #{:custom-formatters} features-to-install)
       (if (is-feature-available? :custom-formatters)
         (custom-formatters/install!)
         (warn-feature-not-available :custom-formatters)))
     (if (some #{:sanity-hints} features-to-install)
       (if (is-feature-available? :sanity-hints)
         (sanity-hints/install!)
         (warn-feature-not-available :sanity-hints))))))

(defn uninstall! []
  (custom-formatters/uninstall!)
  (sanity-hints/uninstall!))

; -- deprecated API ---------------------------------------------------------------------------------------------------------

(defn enable! []
  (.warn js/console (str "devtools.core/enable! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> remove the call")))

(defn disable! []
  (.warn js/console (str "devtools.core/disable! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> remove the call")))

(defn set-single-feature! [_feature _val]
  (.warn js/console (str "devtools.core/set-single-feature! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn enable-single-feature! [_feature]
  (.warn js/console (str "devtools.core/enable-single-feature! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn disable-single-feature! [_feature]
  (.warn js/console (str "devtools.core/disable-single-feature! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn enable-feature! [& _features]
  (.warn js/console (str "devtools.core/enable-feature! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn disable-feature! [& _features]
  (.warn js/console (str "devtools.core/disable-feature! was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn single-feature-available? [_feature]
  (.warn js/console (str "devtools.core/single-feature-available? was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use devtools.core/is-feature-available? instead")))

(defn feature-available? [& _features]
  (.warn js/console (str "devtools.core/feature-available? was removed "
                         "and has no effect in " (make-lib-info) " "
                         "=> use devtools.core/is-feature-available? instead")))