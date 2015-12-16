(ns devtools-sample.config
  (:require [environ.core :refer [env]]))

(defmacro debug? []
  (boolean (env :devtools-debug)))

(defmacro figwheel? []
  (boolean (env :devtools-figwheel)))