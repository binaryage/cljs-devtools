(ns devtools.reporter
  (:require-macros [devtools.oops :refer [oget]])
  (:require [devtools.util :as util]
            [devtools.context :as context]))

(def issues-url "https://github.com/binaryage/cljs-devtools/issues")

; -- internal errors  -------------------------------------------------------------------------------------------------------

(defn report-internal-error! [e & [context footer]]
  (let [console (context/get-console)]
    (try
      (let [message (if (instance? js/Error e)
                      (or (.-message e) e)
                      e)
            header #js ["%cCLJS DevTools Error%c%s"
                        "background-color:red;color:white;font-weight:bold;padding:0px 3px;border-radius:2px;"
                        "color:red"
                        (str " " message)]
            context-msg (str "In " (util/get-lib-info) (if context (str ", " context ".") ".") "\n\n")
            footer-msg (if (some? footer)
                         footer
                         (str "\n\n" "---\n" "Please report the issue here: " issues-url))
            details #js [context-msg e footer-msg]]
        (let [group-collapsed (oget console "groupCollapsed")
              log (oget console "log")
              group-end (oget console "groupEnd")]
          (assert group-collapsed)
          (assert log)
          (assert group-end)
          (.apply group-collapsed console header)
          (.apply log console details)
          (.call group-end console)))
      (catch :default e
        (.error console "FATAL: report-internal-error! failed" e)))))
