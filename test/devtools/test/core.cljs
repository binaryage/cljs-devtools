(ns devtools.test.core
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.core :as devtools]))

(deftest test-install-and-uninstall
  (testing "Testing simple install/uninstall when devtoolsFormatters was not previously set"
    (is (nil? (aget js/window devtools/formatter-key)))
    (is (= (devtools/installed?) false))
    (devtools/install!)
    (is (= (devtools/installed?) true))
    (is (array? (aget js/window devtools/formatter-key)))
    (is (= (.-length (aget js/window devtools/formatter-key)) 1))
    (devtools/uninstall!)
    (is (= (devtools/installed?) false))
    (is (nil? (aget js/window devtools/formatter-key))))
  (testing "Testing simple install/uninstall when devtoolsFormatters was set to empty array"
    (let [empty-formatters #js []]
      (aset js/window devtools/formatter-key empty-formatters)
      (is (= empty-formatters (aget js/window devtools/formatter-key)))
      (is (= (devtools/installed?) false))
      (devtools/install!)
      (is (= (devtools/installed?) true))
      (is (array? (aget js/window devtools/formatter-key)))
      (is (= (.-length (aget js/window devtools/formatter-key)) 1))
      (devtools/uninstall!)
      (is (= (devtools/installed?) false))
      (is (nil? (aget js/window devtools/formatter-key)))))
  (testing "Testing simple install/uninstall when devtoolsFormatters was set to non-empty array"
    (let [existingFormatter #js {"some" "value"}
          initial-formatters #js [existingFormatter]]
      (aset js/window devtools/formatter-key initial-formatters)
      (is (= initial-formatters (aget js/window devtools/formatter-key)))
      (is (= (devtools/installed?) false))
      (devtools/install!)
      (is (= (devtools/installed?) true))
      (is (array? (aget js/window devtools/formatter-key)))
      (is (= (.-length (aget js/window devtools/formatter-key)) 2))
      (devtools/uninstall!)
      (is (= (devtools/installed?) false))
      (is (= (.-length (aget js/window devtools/formatter-key)) 1))
      (is (= existingFormatter (aget (aget js/window devtools/formatter-key) 0))))))