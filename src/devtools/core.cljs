(ns devtools.core
  (:require [devtools.prefs :as prefs]
            [devtools.sanity-hints :as sanity-hints]
            [devtools.custom-formatters :as custom-formatters]
            [devtools.util :as util]
            [goog.userAgent :as ua]))

(def known-features
  {:custom-formatters :install-custom-formatters
   :sanity-hints      :install-sanity-hints})

(defn ^:dynamic missing-feature-warning [feature known-features]
  (str "No such feature '" feature "' is currently available in cljs-devtools. "
       "List of supported features:" (keys known-features)))

(defn ^:dynamic warn-feature-not-available [feature]
  (.warn js/console (str "Feature '" (name feature) "' cannot be installed. Unsupported browser " (ua/getUserAgentString) ".")))

; -- public API -------------------------------------------------------------------------------------------------------------

(defn install! []
  (util/display-banner "Installing cljs-devtools:" known-features)
  (if (prefs/pref :install-custom-formatters)
    (if (custom-formatters/available?)
      (custom-formatters/install!)
      (warn-feature-not-available :custom-formatters)))
  (if (prefs/pref :install-sanity-hints)
    (if (sanity-hints/available?)
      (sanity-hints/install!)
      (warn-feature-not-available :sanity-hints))))

(defn uninstall! []
  (custom-formatters/uninstall!)
  (sanity-hints/uninstall!))

(defn set-prefs! [new-prefs]
  (prefs/set-prefs! new-prefs))

(defn get-prefs []
  (prefs/get-prefs))

(defn set-pref! [pref val]
  (prefs/set-pref! pref val))

(defn set-single-feature! [feature val]
  (if-let [feature-installation-key (feature known-features)]
    (set-pref! feature-installation-key val)
    (.warn js/console (missing-feature-warning feature known-features))))

(defn enable-single-feature! [feature]
  (set-single-feature! feature true))

(defn disable-single-feature! [feature]
  (set-single-feature! feature false))

(defn enable-feature! [& features]
  (doseq [feature features]
    (enable-single-feature! feature)))

(defn disable-feature! [& features]
  (doseq [feature features]
    (disable-single-feature! feature)))

(defn single-feature-available? [feature]
  (case feature
    :custom-formatters (custom-formatters/available?)
    :sanity-hints (sanity-hints/available?)))

(defn feature-available? [& features]
  (every? single-feature-available? features))

; -- deprecated API ---------------------------------------------------------------------------------------------------------

(defn enable! []
  (.warn js/console (str "devtools.core/enable! was deprecated "
                         "and has no effect in current version of cljs-devtools "
                         "=> remove the call")))

(defn disable! []
  (.warn js/console (str "devtools.core/disable! was deprecated "
                         "and has no effect in current version of cljs-devtools "
                         "=> remove the call")))