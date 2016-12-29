(ns devtools.advanced-warning.core
  (:require [devtools.core :as devtools]))

; we assume this will produce advanced build warning
(devtools/install!)

; this should set false to window["devtools-installed"]
(aset js/goog.global "devtools-installed" (devtools/installed?))
