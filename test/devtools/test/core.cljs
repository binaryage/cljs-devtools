(ns devtools.test.core
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.core :as devtools]))

(deftest test-install-and-uninstall
  (testing "Testing install/uninstall when devtoolsFormatters was not previously set"
    (is (nil? (aget js/window devtools/formatter-key)))
    (is (= (devtools/installed?) false))
    (devtools/install!)
    (is (= (devtools/installed?) true))
    (is (array? (aget js/window devtools/formatter-key)))
    (is (= (.-length (aget js/window devtools/formatter-key)) 1))
    (devtools/uninstall!)
    (is (= (devtools/installed?) false))
    (is (nil? (aget js/window devtools/formatter-key))))
  (testing "Testing install/uninstall when devtoolsFormatters was set to an empty array"
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
  (testing "Testing install/uninstall when devtoolsFormatters was set to a non-empty array"
    (let [existing-formatter #js {"some" "value"}
          initial-formatters #js [existing-formatter]]
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
      (is (= existing-formatter (aget js/window devtools/formatter-key 0)))))
  (testing "Testing install, install foreign custom formatter, uninstall scenario"
    (let [existing-formatter #js {"some" "value"}
          initial-formatters #js [existing-formatter]]
      (aset js/window devtools/formatter-key initial-formatters)
      (is (= initial-formatters (aget js/window devtools/formatter-key)))
      (is (= (devtools/installed?) false))
      (devtools/install!)
      (is (= (devtools/installed?) true))
      (is (array? (aget js/window devtools/formatter-key)))
      (is (= (.-length (aget js/window devtools/formatter-key)) 2))
      (let [additional-formatter #js {"this is" "new formatter installed after devtools formatter"}]
        (.push (aget js/window devtools/formatter-key) additional-formatter)
        (is (= (devtools/installed?) true))
        (is (array? (aget js/window devtools/formatter-key)))
        (is (= (.-length (aget js/window devtools/formatter-key)) 3))
        (devtools/uninstall!)
        (is (= (devtools/installed?) false))
        (is (= (.-length (aget js/window devtools/formatter-key)) 2))
        (is (= existing-formatter (aget js/window devtools/formatter-key 0)))
        (is (= additional-formatter (aget js/window devtools/formatter-key 1)))))))