(ns devtools.version)

(def current-version "0.7.2")                                                                                                 ; this should match our project.clj

(defmacro get-current-version []
  current-version)
