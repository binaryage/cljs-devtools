(ns devtools.prefs
  (:require [clojure.string :as string]))

(defmacro color-with-opacity [color opacity]
  (string/replace color "1);" (str opacity ");")))