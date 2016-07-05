(ns devtools-sample.debug
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools.debug :as debug]
            [devtools.custom-formatters :as custom-formatters]))

(log "devtools-sample: enabled debug mode")

(set! custom-formatters/*monitor-enabled* true)
(set! custom-formatters/*sanitizer-enabled* false)

(debug/init!)
