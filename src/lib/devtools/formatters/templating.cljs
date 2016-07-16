(ns devtools.formatters.templating
  (:require [devtools.util :refer-macros [oget oset ocall oapply safe-call]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.helpers :refer [pref]]))

; -- object marking support -------------------------------------------------------------------------------------------------

(defn mark-as-group! [value]
  (specify! value IGroup)
  value)

(defn group? [value]
  (satisfies? IGroup value))

(defn mark-as-template! [value]
  (specify! value ITemplate)
  value)

(defn template? [value]
  (satisfies? ITemplate value))

(defn mark-as-surrogate! [value]
  (specify! value ISurrogate)
  value)

(defn surrogate? [value]
  (satisfies? ISurrogate value))

; ---------------------------------------------------------------------------------------------------------------------------

(defn make-group [& items]
  (let [group (mark-as-group! #js [])]
    (doseq [item items]
      (if (some? item)
        (if (coll? item)
          (.apply (aget group "push") group (mark-as-group! (into-array item)))                                               ; convenience helper to splat cljs collections
          (.push group (pref item)))))
    group))

(defn make-template
  [tag style & children]
  (let [tag (pref tag)
        style (pref style)
        template (mark-as-template! #js [tag (if (empty? style) #js {} #js {"style" style})])]
    (doseq [child children]
      (if (some? child)
        (if (coll? child)
          (.apply (aget template "push") template (mark-as-template! (into-array (keep pref child))))                         ; convenience helper to splat cljs collections
          (if-let [child-value (pref child)]
            (.push template child-value)))))
    template))

(defn concat-templates! [template & templates]
  (mark-as-template! (.apply (oget template "concat") template (into-array (map into-array (keep pref templates))))))

(defn extend-template! [template & args]
  (concat-templates! template args))

(defn make-surrogate
  ([object header] (make-surrogate object header true))
  ([object header has-body] (make-surrogate object header has-body nil))
  ([object header has-body body-template]
   (mark-as-surrogate! (js-obj
                         "target" object
                         "header" header
                         "hasBody" has-body
                         "bodyTemplate" body-template))))

(defn get-target-object [value]
  (if (surrogate? value)
    (oget value "target") value))

; TODO: rewire this
(defn get-current-state []
  (ocall (oget js/window "devtools" "format") "get_current_state"))

(defn make-reference [object & [state-override]]
  (if (nil? object)
    (make-template :span :nil-style :nil-label)                                                                               ; TODO: use (render-json-ml (reusables/nil-markup))
    (let [sub-state (-> (get-current-state)
                        (merge state-override)
                        #_(update :history conj ::reference))]
      (make-group "object" #js {"object" object
                                "config" sub-state}))))

