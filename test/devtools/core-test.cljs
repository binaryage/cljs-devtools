(ns devtools.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.core :as core]))

(deftest test-example
  (testing "Testing example"
    (is (= 1 2))
    (is (= 3 3))))