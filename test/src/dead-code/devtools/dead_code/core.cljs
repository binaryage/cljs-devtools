(ns devtools.dead-code.core
  (:require [devtools.core :as devtools]))

; we use :closure-defines to elide (devtools/install!) calls (see project.clj :dead-code build
; the because there is no other reference to devtools code,
; it should be completely eliminated as dead code in advanced builds

; ^boolean hint is important here
(if ^boolean js/goog.DEBUG (devtools/install!))

; this is an alternative form
(if ^boolean goog/DEBUG (devtools/install!))

; another possible test
(when ^boolean goog/DEBUG (devtools/install!))
