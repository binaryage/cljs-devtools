(ns devtools.test.custom-formatters
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.custom-formatters :as cf]))

(deftest test-install-and-uninstall
  (testing "install/uninstall when devtoolsFormatters was not previously set"
    (is (nil? (aget js/window cf/formatter-key)))
    (is (= (cf/present?) false))
    (cf/install!)
    (is (= (cf/present?) true))
    (is (array? (aget js/window cf/formatter-key)))
    (is (= (.-length (aget js/window cf/formatter-key)) 1))
    (cf/uninstall!)
    (is (= (cf/present?) false))
    (is (nil? (aget js/window cf/formatter-key))))
  (testing "install/uninstall when devtoolsFormatters was set to an empty array"
    (let [empty-formatters #js []]
      (aset js/window cf/formatter-key empty-formatters)
      (is (= empty-formatters (aget js/window cf/formatter-key)))
      (is (= (cf/present?) false))
      (cf/install!)
      (is (= (cf/present?) true))
      (is (array? (aget js/window cf/formatter-key)))
      (is (= (.-length (aget js/window cf/formatter-key)) 1))
      (cf/uninstall!)
      (is (= (cf/present?) false))
      (is (nil? (aget js/window cf/formatter-key)))))
  (testing "install/uninstall when devtoolsFormatters was set to a non-empty array"
    (let [existing-formatter #js {"some" "value"}
          initial-formatters #js [existing-formatter]]
      (aset js/window cf/formatter-key initial-formatters)
      (is (= initial-formatters (aget js/window cf/formatter-key)))
      (is (= (cf/present?) false))
      (cf/install!)
      (is (= (cf/present?) true))
      (is (array? (aget js/window cf/formatter-key)))
      (is (= (.-length (aget js/window cf/formatter-key)) 2))
      (cf/uninstall!)
      (is (= (cf/present?) false))
      (is (= (.-length (aget js/window cf/formatter-key)) 1))
      (is (= existing-formatter (aget js/window cf/formatter-key 0)))))
  (testing "install, install foreign custom formatter, uninstall scenario"
    (let [existing-formatter #js {"some" "value"}
          initial-formatters #js [existing-formatter]]
      (aset js/window cf/formatter-key initial-formatters)
      (is (= initial-formatters (aget js/window cf/formatter-key)))
      (is (= (cf/present?) false))
      (cf/install!)
      (is (= (cf/present?) true))
      (is (array? (aget js/window cf/formatter-key)))
      (is (= (.-length (aget js/window cf/formatter-key)) 2))
      (let [additional-formatter #js {"this is" "new formatter installed after devtools formatter"}]
        (.push (aget js/window cf/formatter-key) additional-formatter)
        (is (= (cf/present?) true))
        (is (array? (aget js/window cf/formatter-key)))
        (is (= (.-length (aget js/window cf/formatter-key)) 3))
        (cf/uninstall!)
        (is (= (cf/present?) false))
        (is (= (.-length (aget js/window cf/formatter-key)) 2))
        (is (= existing-formatter (aget js/window cf/formatter-key 0)))
        (is (= additional-formatter (aget js/window cf/formatter-key 1)))))))