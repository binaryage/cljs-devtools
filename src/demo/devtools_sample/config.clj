(ns devtools-sample.config
  (:require [environ.core :refer [env]]))

(defmacro debug? []
  (boolean (env :devtools-debug)))

(defmacro weasel? []
  (boolean (env :devtools-weasel)))