(ns devtools.core
  (:require [devtools.prefs :as prefs]
            [devtools.sanity-hints :as sanity-hints]
            [devtools.custom-formatters :as custom-formatters]
            [devtools.util :as util]
            [goog.userAgent :as ua]))

(def known-features [:custom-formatters :sanity-hints])
(def features-installed-by-default [:custom-formatters])

(defn ^:dynamic missing-feature-warning [feature known-features]
  (str "No such feature " feature " is currently available in CLJS devtools. "
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
  ([] (install! nil))
  ([features]
   (let [features-to-install (or features features-installed-by-default)]
     (util/display-banner "Installing CLJS devtools with enabled features" features-to-install known-features)
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
                         "and has no effect in current version of CLJS devtools "
                         "=> remove the call")))

(defn disable! []
  (.warn js/console (str "devtools.core/disable! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> remove the call")))

(defn set-single-feature! [_feature _val]
  (.warn js/console (str "devtools.core/set-single-feature! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn enable-single-feature! [_feature]
  (.warn js/console (str "devtools.core/enable-single-feature! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn disable-single-feature! [_feature]
  (.warn js/console (str "devtools.core/disable-single-feature! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn enable-feature! [& _features]
  (.warn js/console (str "devtools.core/enable-feature! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn disable-feature! [& _features]
  (.warn js/console (str "devtools.core/disable-feature! was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use (devtools.core/install! features) to install custom features")))

(defn single-feature-available? [_feature]
  (.warn js/console (str "devtools.core/single-feature-available? was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use devtools.core/is-feature-available? instead")))

(defn feature-available? [& _features]
  (.warn js/console (str "devtools.core/feature-available? was removed "
                         "and has no effect in current version of CLJS devtools "
                         "=> use devtools.core/is-feature-available? instead")))