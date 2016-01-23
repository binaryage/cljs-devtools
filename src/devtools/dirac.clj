(ns devtools.dirac
  (:require [environ.core :refer [env]]))

(def ^:dynamic *dirac-agent-host* (env :dirac-agent-host))
(def ^:dynamic *dirac-agent-port* (env :dirac-agent-port))
(def ^:dynamic *dirac-agent-verbose* (env :dirac-agent-verbose))
(def ^:dynamic *dirac-agent-auto-connect* (env :dirac-agent-auto-connect))
(def ^:dynamic *dirac-agent-response-timeout* (env :dirac-agent-response-timeout))
(def ^:dynamic *dirac-weasel-verbose* (env :dirac-weasel-verbose))
(def ^:dynamic *dirac-weasel-auto-connect* (env :dirac-weasel-auto-connect))
(def ^:dynamic *dirac-weasel-pre-eval-delay* (env :dirac-weasel-pre-eval-delay))

(defmacro gen-config []
  (merge {}
         (if *dirac-agent-host* [:dirac-agent-host (str *dirac-agent-host*)])
         (if *dirac-agent-port* [:dirac-agent-port (int *dirac-agent-port*)])
         (if *dirac-agent-verbose* [:dirac-agent-verbose (boolean *dirac-agent-verbose*)])
         (if *dirac-agent-auto-connect* [:dirac-agent-auto-connect (boolean *dirac-agent-auto-connect*)])
         (if *dirac-agent-response-timeout* [:dirac-agent-response-timeout (int *dirac-agent-response-timeout*)])
         (if *dirac-weasel-verbose* [:dirac-weasel-verbose (boolean *dirac-weasel-verbose*)])
         (if *dirac-weasel-auto-connect* [:dirac-weasel-auto-connect (boolean *dirac-weasel-auto-connect*)])
         (if *dirac-weasel-pre-eval-delay* [:dirac-weasel-pre-eval-delay (int *dirac-weasel-pre-eval-delay*)])))