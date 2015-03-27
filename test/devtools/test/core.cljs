(ns devtools.test.core
  (:require [cljs.test :refer-macros [deftest testing is]]
            [devtools.core :as core]))

(deftest test-example
  (testing "Testing example"
    (is (= 1 1))
    ))