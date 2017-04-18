(ns devtools.toolbox
  "Some convenience macros for situations when you want to call toolbox functions optionally under :advanced mode."
  (:require [devtools.oops :refer [gen-oget]]))

(defn gen-get-toolbox-method [method]
  (gen-oget 'goog/global "devtools" "toolbox" (name method)))

; -- public macros ----------------------------------------------------------------------------------------------------------

(defmacro envelope [& args]
  `(let [args# (cljs.core/array ~@args)]
     (if-let [method# ~(gen-get-toolbox-method "envelope")]
       (.apply method# nil args#)
       args#)))

(defmacro force-format [obj]
  `(let [obj# ~obj]
     (if-let [method# ~(gen-get-toolbox-method "force_format")]
       (method# obj#)
       obj#)))
