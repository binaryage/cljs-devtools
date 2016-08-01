(ns devtools.tests.formatters
  (:require-macros [devtools.tests.utils.macros :refer [with-prefs]])
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.formatters :as cf]
            [devtools.core]
            [devtools.util :refer [formatter-key]]))

(deftest test-install-and-uninstall
  ; we have no straight-forward way how to test custom formatter detector
  (with-prefs {:dont-detect-custom-formatters true}
    (set! cf/available? (constantly true))
    (testing "install/uninstall when devtoolsFormatters was not previously set"
      (is (nil? (aget js/window formatter-key)))
      (is (= (cf/present?) false))
      (cf/install!)
      (is (= (cf/present?) true))
      (is (array? (aget js/window formatter-key)))
      (is (= (.-length (aget js/window formatter-key)) 1))
      (cf/uninstall!)
      (is (= (cf/present?) false))
      (is (nil? (aget js/window formatter-key))))
    (testing "install/uninstall when devtoolsFormatters was set to an empty array"
      (let [empty-formatters #js []]
        (aset js/window formatter-key empty-formatters)
        (is (= empty-formatters (aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (aget js/window formatter-key)))
        (is (= (.-length (aget js/window formatter-key)) 1))
        (cf/uninstall!)
        (is (= (cf/present?) false))
        (is (nil? (aget js/window formatter-key)))))
    (testing "install/uninstall when devtoolsFormatters was set to a non-empty array"
      (let [existing-formatter #js {"some" "value"}
            initial-formatters #js [existing-formatter]]
        (aset js/window formatter-key initial-formatters)
        (is (= initial-formatters (aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (aget js/window formatter-key)))
        (is (= (.-length (aget js/window formatter-key)) 2))
        (cf/uninstall!)
        (is (= (cf/present?) false))
        (is (= (.-length (aget js/window formatter-key)) 1))
        (is (= existing-formatter (aget js/window formatter-key 0)))))
    (testing "install, install foreign custom formatter, uninstall scenario"
      (let [existing-formatter #js {"some" "value"}
            initial-formatters #js [existing-formatter]]
        (aset js/window formatter-key initial-formatters)
        (is (= initial-formatters (aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (aget js/window formatter-key)))
        (is (= (.-length (aget js/window formatter-key)) 2))
        (let [additional-formatter #js {"this is" "new formatter installed after devtools formatter"}]
          (.push (aget js/window formatter-key) additional-formatter)
          (is (= (cf/present?) true))
          (is (array? (aget js/window formatter-key)))
          (is (= (.-length (aget js/window formatter-key)) 3))
          (cf/uninstall!)
          (is (= (cf/present?) false))
          (is (= (.-length (aget js/window formatter-key)) 2))
          (is (= existing-formatter (aget js/window formatter-key 0)))
          (is (= additional-formatter (aget js/window formatter-key 1))))))))
