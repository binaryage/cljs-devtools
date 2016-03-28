(ns devtools.toolbox
  (:require [devtools.format :as format]
            [devtools.prefs :as prefs]))

(defn envelope
  "This is a simple wrapper for logging \"busy\" objects.
  This is especially handy when you happen to be logging javascript objects with many properties.
  Standard javascript console renderer tends to print a lot of infomation in the header in some cases and that can make
  console output pretty busy. By using envelope you can force your own short header and let the details expand on demand
  via a disclosure triangle. The header can be styled and you can optionally specify preferred wrapping tag (advanced)."
  ([obj]
   (envelope obj (prefs/pref :default-envelope-header)))
  ([obj header]
   (envelope obj header ""))
  ([obj header style]
   (envelope obj header style "span"))
  ([obj header style tag]
   (reify
     format/IDevtoolsFormat
     (-header [_] (format/template tag style header))
     (-has-body [_] true)
     (-body [_] (format/standard-reference obj)))))