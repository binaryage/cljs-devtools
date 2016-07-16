(ns devtools.core
  (:require [devtools.prefs :as prefs]
            [devtools.sanity-hints :as sanity-hints]
            [devtools.formatters :as formatters]
            [devtools.async :as async]
            [devtools.util :refer [display-banner-if-needed! install-feature! resolve-features! make-lib-info
                                   print-config-overrides-if-requested!]]))

(def known-features [:custom-formatters :sanity-hints :async])
(def default-features [:custom-formatters])
(def feature-groups {:all     known-features
                     :default default-features})

; -- public API -------------------------------------------------------------------------------------------------------------

(defn is-feature-available? [feature]
  (case feature
    :custom-formatters (formatters/available?)
    :sanity-hints (sanity-hints/available?)
    :async (sanity-hints/available?)))

(defn available?
  ([] (available? (prefs/pref :features-to-install)))
  ([features-desc]
   (let [features (resolve-features! features-desc feature-groups)]
     (if (empty? features)
       false
       (every? is-feature-available? features)))))

(defn is-feature-installed? [feature]
  (case feature
    :custom-formatters (formatters/installed?)
    :sanity-hints (sanity-hints/installed?)
    :async (async/installed?)))

(defn installed?
  ([] (installed? (prefs/pref :features-to-install)))
  ([features-desc]
   (let [features (resolve-features! features-desc feature-groups)]
     (if (empty? features)
       false
       (every? is-feature-installed? features)))))

(defn install!
  ([] (install! (prefs/pref :features-to-install)))
  ([features-desc]
   (let [features (resolve-features! features-desc feature-groups)]
     (display-banner-if-needed! features feature-groups)
     (print-config-overrides-if-requested! "config overrides prior install:\n")
     (install-feature! :custom-formatters features is-feature-available? formatters/install!)
     (install-feature! :sanity-hints features is-feature-available? sanity-hints/install!)
     (install-feature! :async features is-feature-available? async/install!))))

(defn uninstall! []
  (formatters/uninstall!)
  (sanity-hints/uninstall!)
  (async/uninstall!))

(defn set-prefs! [new-prefs]
  (prefs/set-prefs! new-prefs))

(defn get-prefs []
  (prefs/get-prefs))

(defn set-pref! [pref val]
  (prefs/set-pref! pref val))

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
