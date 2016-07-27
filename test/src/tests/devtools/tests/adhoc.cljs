(ns devtools.tests.adhoc
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.utils.macros :refer [range = > < + str want? with-prefs]])                                       ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.pseudo.tag :as tag]
            [devtools.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                         remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.formatters.helpers :refer [cljs-function?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.utils.batteries :as b :refer [REF NATIVE-REF]]))

(deftest test-strings
  (testing "short strings"
    (is-header "some short string"
      [::tag/cljs-land
       [::tag/header
        [::tag/string (str :dq "some short string" :dq)]]])
    (is-header "line1\nline2\n\nline4"
      [::tag/cljs-land
       [::tag/header
        [::tag/string (str :dq "line1" :new-line-string-replacer "line2" :new-line-string-replacer :new-line-string-replacer "line4" :dq)]]]))
  (testing "long strings"
    (is-header "123456789012345678901234567890123456789012345678901234567890"
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/string (str :dq "12345678901234567890" :string-abbreviation-marker "12345678901234567890" :dq)]]])))
    (is-header "1234\n6789012345678901234567890123456789012345678901234\n67890"
      [::tag/cljs-land
       [::tag/header
        REF]]
      (fn [ref]
        (is (surrogate? ref))
        (is-header ref
          [::tag/expandable
           [::tag/expandable-inner
            [::tag/string
             (str
               :dq
               "1234" :new-line-string-replacer "678901234567890"
               :string-abbreviation-marker
               "12345678901234" :new-line-string-replacer "67890"
               :dq)]]])
        (is-body ref
          [::tag/expanded-string
           (str
             "1234" :new-line-string-replacer
             "\n6789012345678901234567890123456789012345678901234" :new-line-string-replacer
             "\n67890")])))))
