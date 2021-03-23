(ns devtools.tests.core
  (:require-macros [devtools.tests.utils.macros :refer [with-prefs]])
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [devtools.tests.utils.test :refer [with-captured-console
                                         clear-captured-console-output!
                                         get-captured-console-messages]]
            [devtools.core :as devtools]
            [devtools.formatters :as formatters]
            [devtools.hints :as hints]
            [devtools.async :as async]))

(use-fixtures :once with-captured-console)

(deftest test-core-install-and-uninstall
  ; we have no straight-forward way how to test custom formatter detector
  (with-prefs {:dont-detect-custom-formatters true}
    (set! formatters/available? (constantly true))                                                                            ; this is needed to fake availability check in phantomjs env
    (testing "install/uninstall :all features"
      (devtools/install! :all)
      (is (= (devtools/installed? :all) true))
      (is (= (devtools/installed? :formatters) true))
      (is (= (devtools/installed? :hints) true))
      (devtools/uninstall!)
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) false)))
    (testing "install/uninstall no features"
      (devtools/install! [])
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) false)))
    (testing "install/uninstall :default features"
      (devtools/install! :default)
      (is (= (devtools/installed? :default) true))
      (devtools/uninstall!)
      (is (= (devtools/installed? :default) false)))
    (testing "install/uninstall :custom-formatters feature"
      (devtools/install! [:formatters])
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) true))
      (is (= (devtools/installed? :hints) false))
      (devtools/uninstall!)
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) false)))
    (testing "install/uninstall :sanity-hints feature"
      (devtools/install! [:hints])
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) true))
      (devtools/uninstall!)
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) false)))
    (testing "double-install/single uninstall :all features"
      (devtools/install! :all)
      (devtools/install! :all)
      (is (= (devtools/installed? :all) true))
      (is (= (devtools/installed? :formatters) true))
      (is (= (devtools/installed? :hints) true))
      (devtools/uninstall!)
      (is (= (devtools/installed? :all) false))
      (is (= (devtools/installed? :formatters) false))
      (is (= (devtools/installed? :hints) false)))
    (testing "banner printing during install"
      (with-prefs {:dont-display-formatters-removal-warning true}
        (clear-captured-console-output!)
        (devtools/install! [:formatters])
        (is (re-matches #".*Installing.*CLJS DevTools.*" (last (get-captured-console-messages))))
        (devtools/uninstall!)))
    (testing "banner printing suppression during install"
      (with-prefs {:dont-display-banner true
                   :dont-display-formatters-removal-warning true}
        (clear-captured-console-output!)
        (devtools/install! [:formatters])
        (is (nil? (last (get-captured-console-messages))))
        (devtools/uninstall!)))
    (binding [formatters/available? (constantly false)
              hints/available? (constantly false)
              async/available? (constantly false)]
      (testing "working availability checks"
        (clear-captured-console-output!)
        (devtools/install! :all)
        (is (= (count (get-captured-console-messages)) 4))
        (is (every? #(re-matches #".*cannot be installed.*" %) (rest (get-captured-console-messages))))
        (devtools/uninstall!))
      (testing "bypass availability checks"
        (with-prefs {:bypass-availability-checks true
                     :dont-display-formatters-removal-warning true}
          (clear-captured-console-output!)
          (devtools/install! :all)
          (is (= (devtools/installed? :all) true))
          (is (= (devtools/installed? :formatters) true))
          (is (= (devtools/installed? :hints) true))
          (devtools/uninstall!))))))
