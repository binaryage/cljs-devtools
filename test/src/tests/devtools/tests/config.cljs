(ns devtools.tests.config
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.prefs :refer [default-prefs merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.core :refer [installed?]]))

(deftest test-config
  (testing "expected config overrides passed via compiler options are present"                                                ; see project.clj :tests-with-config profile >
    (is (= (pref :features-to-install) [:sanity-hints]))
    (is (= (installed? :sanity-hints) true))
    (is (= (installed? :custom-formatters) false))
    (is (= (pref :fn-symbol) "F"))
    (is (= (pref :print-config-overrides) true))))
