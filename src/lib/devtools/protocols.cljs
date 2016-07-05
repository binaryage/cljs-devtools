(ns devtools.protocols)

(defprotocol ITemplate
  "Marker protocol indicating a devtools template.")

(defprotocol IGroup
  "Marker protocol indicating a devtools group.")

(defprotocol ISurrogate
  "Marker protocol indicating a devtools surrogate object.")

(defprotocol IFormat
  (-header [value])
  (-has-body [value])
  (-body [value]))
