(ns devtools.advanced-warning.core
  (:require-macros [devtools.oops :refer [unchecked-aset]])
  (:require [devtools.core :as devtools]
            [devtools.context :as context]))

; we assume this will produce advanced build warning
(devtools/install!)

; this should set false to window["devtools-installed"]
(unchecked-aset (context/get-root) "devtools-installed" (devtools/installed?))
