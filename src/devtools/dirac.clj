(ns devtools.dirac
  (:require [environ.core :refer [env]]))

(def ^:dynamic *dirac-agent-host* (env :dirac-agent-host))
(def ^:dynamic *dirac-agent-port* (env :dirac-agent-port))
(def ^:dynamic *dirac-agent-verbose* (env :dirac-agent-verbose))
(def ^:dynamic *dirac-agent-auto-reconnect* (env :dirac-agent-auto-reconnect))
(def ^:dynamic *dirac-agent-response-timeout* (env :dirac-agent-response-timeout))
(def ^:dynamic *dirac-weasel-verbose* (env :dirac-weasel-verbose))
(def ^:dynamic *dirac-weasel-auto-reconnect* (env :dirac-weasel-auto-reconnect))
(def ^:dynamic *dirac-weasel-pre-eval-delay* (env :dirac-weasel-pre-eval-delay))

(defmacro gen-config []
  (merge {}
         (if *dirac-agent-host* [:dirac-agent-host (str *dirac-agent-host*)])
         (if *dirac-agent-port* [:dirac-agent-port (int *dirac-agent-port*)])
         (if *dirac-agent-verbose* [:dirac-agent-verbose (boolean *dirac-agent-verbose*)])
         (if *dirac-agent-auto-reconnect* [:dirac-agent-auto-reconnect (boolean *dirac-agent-auto-reconnect*)])
         (if *dirac-agent-response-timeout* [:dirac-agent-response-timeout (int *dirac-agent-response-timeout*)])
         (if *dirac-weasel-verbose* [:dirac-weasel-verbose (boolean *dirac-weasel-verbose*)])
         (if *dirac-weasel-auto-reconnect* [:dirac-weasel-auto-reconnect (boolean *dirac-weasel-auto-reconnect*)])
         (if *dirac-weasel-pre-eval-delay* [:dirac-weasel-pre-eval-delay (int *dirac-weasel-pre-eval-delay*)])))