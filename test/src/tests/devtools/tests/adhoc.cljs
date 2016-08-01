(ns devtools.tests.adhoc
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str want? with-prefs]])                                       ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.protocols :refer [IFormat]]
            [devtools.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                         remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.formatters.helpers :refer [cljs-function?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.utils.batteries :as b :refer [REF NATIVE-REF]]))

;(defn gen-nested-template [n]
;  (if (pos? n)
;    #js ["span" #js {} (gen-nested-template (dec n))]
;    #js ["span" #js {} "X"]))
;
;(deftype X1 [n]
;  IFormat
;  (-header [value]
;    (gen-nested-template n))
;  (-has-body [value] false)
;  (-body [value]))
;
;(deftest test-issue22
;  (testing "long"
;    (let [l14 (X1. 12)]
;      (is-header l14
;        []))))

