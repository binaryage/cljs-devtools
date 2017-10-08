(ns devtools.version)

(def current-version "0.9.7")                                                                                        ; this should match our project.clj

(defmacro get-current-version []
  current-version)
