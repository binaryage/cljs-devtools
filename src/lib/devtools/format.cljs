(ns devtools.format)

; WARNING this namespace is here for legacy reasons, it will be removed in future!

; ---------------------------------------------------------------------------------------------------------------------------
; PROTOCOL SUPPORT

(defprotocol ^:deprecated IDevtoolsFormat                                                                                     ; use IFormat instead
  (-header [value])
  (-has-body [value])
  (-body [value]))
