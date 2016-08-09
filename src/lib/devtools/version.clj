(ns devtools.version)

(def current-version "0.8.2-SNAPSHOT")                                                                                        ; this should match our project.clj

(defmacro get-current-version []
  current-version)
