(ns devtools.tests.core
  (:require-macros [devtools.utils.macros :refer [with-prefs]])
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [devtools.utils.test :refer [with-captured-console
                                         clear-captured-console-output!
                                         get-captured-console-messages]]
            [devtools.core :as devtools]
            [devtools.custom-formatters :as custom-formatters]
            [devtools.sanity-hints :as sanity-hints]))

(use-fixtures :once with-captured-console)

(deftest test-core-install-and-uninstall
  (set! custom-formatters/available? (constantly true))                                                                       ; this is needed to fake availability check in phantomjs env
  (testing "install/uninstall :all features"
    (devtools/install! :all)
    (is (= (devtools/installed? :all) true))
    (is (= (devtools/installed? :custom-formatters) true))
    (is (= (devtools/installed? :sanity-hints) true))
    (devtools/uninstall!)
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) false)))
  (testing "install/uninstall no features"
    (devtools/install! [])
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) false)))
  (testing "install/uninstall :default features"
    (devtools/install! :default)
    (is (= (devtools/installed? :default) true))
    (devtools/uninstall!)
    (is (= (devtools/installed? :default) false)))
  (testing "install/uninstall :custom-formatters feature"
    (devtools/install! [:custom-formatters])
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) true))
    (is (= (devtools/installed? :sanity-hints) false))
    (devtools/uninstall!)
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) false)))
  (testing "install/uninstall :sanity-hints feature"
    (devtools/install! [:sanity-hints])
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) true))
    (devtools/uninstall!)
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) false)))
  (testing "double-install/single uninstall :all features"
    (devtools/install! :all)
    (devtools/install! :all)
    (is (= (devtools/installed? :all) true))
    (is (= (devtools/installed? :custom-formatters) true))
    (is (= (devtools/installed? :sanity-hints) true))
    (devtools/uninstall!)
    (is (= (devtools/installed? :all) false))
    (is (= (devtools/installed? :custom-formatters) false))
    (is (= (devtools/installed? :sanity-hints) false)))
  (testing "banner printing during install"
    (clear-captured-console-output!)
    (devtools/install! [:custom-formatters])
    (is (re-matches #".*Installing.*CLJS DevTools.*" (last (get-captured-console-messages))))
    (devtools/uninstall!))
  (testing "banner printing supression during install"
    (with-prefs {:dont-display-banner true}
      (clear-captured-console-output!)
      (devtools/install! [:custom-formatters])
      (is (nil? (last (get-captured-console-messages))))
      (devtools/uninstall!)))
