(ns devtools-sample.issue23
  (:require-macros [devtools-sample.logging :refer [log info]])
  (:require [devtools-sample.boot :refer [boot!]]
            [devtools.protocols :refer [IFormat]]))

(boot! "/src/demo/devtools_sample/issue23.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(throw (ex-info "I'm ex-info" {:with "some data"}))

; <-- MEAT STOPS HERE ---
