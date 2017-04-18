(ns devtools.reporter
  (:require-macros [devtools.oops :refer [oget]])
  (:require [devtools.util :as util]))

(def issues-url "https://github.com/binaryage/cljs-devtools/issues")

; -- internal errors  -------------------------------------------------------------------------------------------------------

(defn report-internal-error! [e & [context footer]]
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
      (let [c js/console
            group-collapsed (oget c "groupCollapsed")
            log (oget c "log")
            group-end (oget c "groupEnd")]
        (assert group-collapsed)
        (assert log)
        (assert group-end)
        (.apply group-collapsed c header)
        (.apply log c details)
        (.call group-end c)))
    (catch :default e
      (.error js/console "FATAL: report-internal-error! failed" e))))
