(ns devtools.core
  (:require [devtools.prefs :as prefs]
            [devtools.sanity-hints :as hints]
            [devtools.custom-formatters :as custom-formatters]
            [devtools.dirac :as dirac]
            [devtools.util :as util]))

(def known-features
  {:custom-formatters :install-custom-formatters
   :dirac             :install-dirac-support
   :sanity-hints      :install-sanity-hints})

(defn ^:dynamic missing-feature-warning [feature known-features]
  (str "No such feature '" feature "' is currently available in cljs-devtools. "
       "List of supported features:" (keys known-features)))

; -- public API -------------------------------------------------------------------------------------------------------------

(defn install! []
  (util/display-banner "Installing cljs-devtools:" known-features)
  (if (prefs/pref :install-custom-formatters)
    (custom-formatters/install!))
  (if (prefs/pref :install-sanity-hints)
    (hints/install!))
  (if (prefs/pref :install-dirac-support)
    (dirac/install!)))

(defn uninstall! []
  (custom-formatters/uninstall!)
  (hints/uninstall!)
  (dirac/uninstall!))

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