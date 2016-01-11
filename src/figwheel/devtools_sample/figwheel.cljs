(ns devtools-sample.figwheel
  (:require [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------
; has to be included before boot

(def config
  {:websocket-url "ws://localhost:7000/figwheel-ws"})

(defn start! []
  (figwheel/start config))

(start!)