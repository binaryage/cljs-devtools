(ns devtools.tests.formatters
  (:require-macros [devtools.tests.utils.macros :refer [with-prefs]]
                   [devtools.oops :refer [unchecked-aset unchecked-aget]])
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.formatters :as cf]
            [devtools.core]
            [devtools.util :refer [formatter-key]]))

(deftest test-install-and-uninstall
  ; we have no straight-forward way how to test custom formatter detector
  (with-prefs {:dont-detect-custom-formatters true}
    (set! cf/available? (constantly true))
    (testing "install/uninstall when devtoolsFormatters was not previously set"
      (is (nil? (unchecked-aget js/window formatter-key)))
      (is (= (cf/present?) false))
      (cf/install!)
      (is (= (cf/present?) true))
      (is (array? (unchecked-aget js/window formatter-key)))
      (is (= (.-length (unchecked-aget js/window formatter-key)) 1))
      (cf/uninstall!)
      (is (= (cf/present?) false))
      (is (nil? (unchecked-aget js/window formatter-key))))
    (testing "install/uninstall when devtoolsFormatters was set to an empty array"
      (let [empty-formatters #js []]
        (unchecked-aset js/window formatter-key empty-formatters)
        (is (= empty-formatters (unchecked-aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (unchecked-aget js/window formatter-key)))
        (is (= (.-length (unchecked-aget js/window formatter-key)) 1))
        (cf/uninstall!)
        (is (= (cf/present?) false))
        (is (nil? (unchecked-aget js/window formatter-key)))))
    (testing "install/uninstall when devtoolsFormatters was set to a non-empty array"
      (let [existing-formatter #js {"some" "value"}
            initial-formatters #js [existing-formatter]]
        (unchecked-aset js/window formatter-key initial-formatters)
        (is (= initial-formatters (unchecked-aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (unchecked-aget js/window formatter-key)))
        (is (= (.-length (unchecked-aget js/window formatter-key)) 2))
        (cf/uninstall!)
        (is (= (cf/present?) false))
        (is (= (.-length (unchecked-aget js/window formatter-key)) 1))
        (is (= existing-formatter (unchecked-aget js/window formatter-key 0)))))
    (testing "install, install foreign custom formatter, uninstall scenario"
      (let [existing-formatter #js {"some" "value"}
            initial-formatters #js [existing-formatter]]
        (unchecked-aset js/window formatter-key initial-formatters)
        (is (= initial-formatters (unchecked-aget js/window formatter-key)))
        (is (= (cf/present?) false))
        (cf/install!)
        (is (= (cf/present?) true))
        (is (array? (unchecked-aget js/window formatter-key)))
        (is (= (.-length (unchecked-aget js/window formatter-key)) 2))
        (let [additional-formatter #js {"this is" "new formatter installed after devtools formatter"}]
          (.push (unchecked-aget js/window formatter-key) additional-formatter)
          (is (= (cf/present?) true))
          (is (array? (unchecked-aget js/window formatter-key)))
          (is (= (.-length (unchecked-aget js/window formatter-key)) 3))
          (cf/uninstall!)
          (is (= (cf/present?) false))
          (is (= (.-length (unchecked-aget js/window formatter-key)) 2))
          (is (= existing-formatter (unchecked-aget js/window formatter-key 0)))
          (is (= additional-formatter (unchecked-aget js/window formatter-key 1))))))))
