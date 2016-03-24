(ns devtools-sample.core
  (:require [devtools.core :as devtools]))

;(if js/goog.DEBUG (devtools/install!))
(if goog/DEBUG (devtools/install!))