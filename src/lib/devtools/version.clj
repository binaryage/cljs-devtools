(ns devtools.version)

(def current-version "0.7.3-SNAPSHOT")                                                                                        ; this should match our project.clj

(defmacro get-current-version []
  current-version)
