(ns devtools.dirac
  (:require [environ.core :refer [env]]))

(def ^:dynamic *dirac-agent-host* (env :dirac-agent-host))
(def ^:dynamic *dirac-agent-port* (env :dirac-agent-port))
(def ^:dynamic *dirac-agent-verbose* (env :dirac-agent-verbose))
(def ^:dynamic *dirac-agent-auto-connect* (env :dirac-agent-auto-connect))
(def ^:dynamic *dirac-agent-response-timeout* (env :dirac-agent-response-timeout))

(defmacro gen-config []
  (merge {}
         (if *dirac-agent-host* [:dirac-agent-host (str *dirac-agent-host*)])
         (if *dirac-agent-port* [:dirac-agent-port (int *dirac-agent-port*)])
         (if *dirac-agent-verbose* [:dirac-agent-verbose (boolean *dirac-agent-verbose*)])
         (if *dirac-agent-auto-connect* [:dirac-agent-auto-connect (boolean *dirac-agent-auto-connect*)])
         (if *dirac-agent-response-timeout* [:dirac-agent-response-timeout (int *dirac-agent-response-timeout*)])))