(ns devtools.dirac
  (:require [environ.core :refer [env]]))

(def ^:dynamic *dirac-agent-host* (env :dirac-agent-host))
(def ^:dynamic *dirac-agent-port* (env :dirac-agent-port))

(defmacro gen-config []
  (merge {}
         (if *dirac-agent-host* [:dirac-agent-host (str *dirac-agent-host*)])
         (if *dirac-agent-port* [:dirac-agent-port (int *dirac-agent-port*)])))