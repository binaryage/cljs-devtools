(ns devtools-sample.debug
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools.debug :as debug]
            [devtools.formatters :as formatters]))

(log "devtools-sample: enabled debug mode")

(set! formatters/*monitor-enabled* true)
(set! formatters/*sanitizer-enabled* false)

(debug/init!)
