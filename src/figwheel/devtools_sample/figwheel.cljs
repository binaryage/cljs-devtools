(ns devtools-sample.figwheel
  (:require [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------
; has to be included before boot

(defn start! []
  (figwheel/start
    {:websocket-url "ws://localhost:7000/figwheel-ws"}))

(start!)
