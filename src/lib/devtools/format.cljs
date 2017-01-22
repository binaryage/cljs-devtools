(ns devtools.format
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.context :as context]))

; WARNING this namespace is here for legacy reasons, it will be removed in future!

; ---------------------------------------------------------------------------------------------------------------------------
; PROTOCOL SUPPORT

(defprotocol ^:deprecated IDevtoolsFormat                                                                                     ; use IFormat instead
  (-header [value])
  (-has-body [value])
  (-body [value]))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(def ^:dynamic *setup-done*)

(defn setup! []
  (when-not *setup-done*
    (set! *setup-done* true)

    ; note: we cannote require devtools.formatters.templating or .markup because that would lead to circular requires
    (def make-template-fn (oget (context/get-root) "devtools" "formatters" "templating" "make_template"))
    (def make-group-fn (oget (context/get-root) "devtools" "formatters" "templating" "make_group"))
    (def make-reference-fn (oget (context/get-root) "devtools" "formatters" "templating" "make_reference"))
    (def make-surrogate-fn (oget (context/get-root) "devtools" "formatters" "templating" "make_surrogate"))
    (def render-markup-fn (oget (context/get-root) "devtools" "formatters" "templating" "render_markup"))
    (def <header>-fn (oget (context/get-root) "devtools" "formatters" "markup" "_LT_header_GT_"))
    (def <standard-body>-fn (oget (context/get-root) "devtools" "formatters" "markup" "_LT_standard_body_GT_"))

    (assert make-template-fn)
    (assert make-group-fn)
    (assert make-reference-fn)
    (assert make-surrogate-fn)
    (assert render-markup-fn)
    (assert <header>-fn)
    (assert <standard-body>-fn)))

(defn- render-markup [& args]
  (setup!)
  (apply render-markup-fn args))

; ---------------------------------------------------------------------------------------------------------------------------

; deprecated functionality, implemented for easier transition from v0.7.x to v0.8

(defn ^:deprecated make-template [& args]
  (setup!)
  (apply make-template-fn args))

(defn ^:deprecated make-group [& args]
  (setup!)
  (apply make-group-fn args))

(defn ^:deprecated make-surrogate [& args]
  (setup!)
  (apply make-surrogate-fn args))

(defn ^:deprecated template [& args]
  (setup!)
  (apply make-template-fn args))

(defn ^:deprecated group [& args]
  (setup!)
  (apply make-group-fn args))

(defn ^:deprecated surrogate [& args]
  (setup!)
  (apply make-surrogate-fn args))

(defn ^:deprecated reference [object & [state-override]]
  (setup!)
  (apply make-reference-fn [object #(merge % state-override)]))

(defn ^:deprecated standard-reference [target]
  (setup!)
  (make-template-fn :ol :standard-ol-style (make-template-fn :li :standard-li-style (make-reference-fn target))))

(defn ^:deprecated build-header [& args]
  (setup!)
  (render-markup (apply <header>-fn args)))

(defn ^:deprecated standard-body-template [lines & rest]
  (setup!)
  (let [args (concat [(map (fn [x] [x]) lines)] rest)]
    (render-markup (apply <standard-body>-fn args))))
