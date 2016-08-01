(ns devtools.tests.adhoc
  (:refer-clojure :exclude [range = > < + str])
  (:require-macros [devtools.tests.utils.macros :refer [range = > < + str want? with-prefs]])                                 ; prefs aware versions
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [devtools.protocols :refer [IFormat]]
            [devtools.tests.utils.test :refer [reset-prefs-to-defaults! js-equals is-header is-body has-body? unroll
                                               remove-empty-styles pref-str]]
            [devtools.formatters.core :refer [header-api-call has-body-api-call body-api-call]]
            [devtools.formatters.templating :refer [surrogate?]]
            [devtools.formatters.helpers :refer [cljs-function?]]
            [devtools.prefs :refer [merge-prefs! set-pref! set-prefs! update-pref! get-prefs pref]]
            [devtools.tests.env.core :as env :refer [REF NATIVE-REF]]))
