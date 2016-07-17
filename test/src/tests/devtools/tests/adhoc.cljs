(ns devtools.tests.adhoc
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str want?]])                                                  ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.pseudo.style :as style]
            [devtools.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                         remove-empty-styles pref-str]]
            [devtools.formatters.core :as f :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.utils.batteries :as b :refer [REF NATIVE-REF]]))

(deftest test-deftype
  (testing "simple deftype"
    (let [type-instance (b/SimpleType. "some-value")]
      (is-header type-instance
        ["span" ::style/cljs-land
         ["span" ::style/header
          NATIVE-REF]]))))                                                                                                    ; TODO!

