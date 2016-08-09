(ns devtools.version)

(def current-version "0.8.1")                                                                                                 ; this should match our project.clj

(defmacro get-current-version []
  current-version)
