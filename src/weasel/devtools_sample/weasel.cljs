(ns devtools-sample.weasel
  (:require [weasel.repl :as repl]))

(.log js/console "WEASEL")

(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))

