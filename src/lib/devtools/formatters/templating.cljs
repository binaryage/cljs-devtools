(ns devtools.formatters.templating
  (:require [cljs.pprint :refer [pprint]]
            [devtools.util :refer-macros [oget oset ocall oapply safe-call]]
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

(defn reference? [value]
  (and (group? value)
       (= (aget value 0) "object")))

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
  (ocall (oget js/window "devtools" "formatters" "core") "get_current_state"))

(defn make-reference [object & [state-override]]
  (if (nil? object)
    (make-template :span :nil-style :nil-label)                                                                               ; TODO: use (render-json-ml (reusables/nil-markup))
    (let [sub-state (-> (get-current-state)
                        (merge state-override)
                        #_(update :history conj ::reference))]
      (make-group "object" #js {"object" object
                                "config" sub-state}))))

; -- JSON ML support --------------------------------------------------------------------------------------------------------

; a renderer from hiccup-like data markup to json-ml
;
; [[tag style] child1 child2 ...] -> #js [tag #js {"style" ...} child1 child2 ...]
;

(declare render-json-ml)
;
;(defn special? [markup]
;  {:pre [(sequential? markup)]}
;  (string? (first markup)))
;
;(defn json-ml-renderer [markup]
;  {:pre [(sequential? markup)]}
;  (let [tag-info (pref (first markup))
;        children (rest markup)]
;    (case tag-info
;      "surrogate" (let [obj (first children)
;                        converted-children (map render-json-ml (rest children))]
;                    (apply make-surrogate (concat [obj] converted-children)))
;      "reference" (apply make-reference children)
;      (let [[tag style] tag-info]
;        (apply make-template tag style (keep pref children))))))
;
;(defn render-json-ml2 [markup]
;  (if (sequential? markup)
;    (let [converted-markup (if (special? markup)
;                             markup
;                             (map render-json-ml markup))]
;      (json-ml-renderer converted-markup))
;    markup))

(def ^:mutable *current-markup* nil)

(defn pprint-markup [markup]
  (with-out-str (pprint markup)))

(defn assert-markup-error [msg]
  (assert false (str msg "\n" (pprint-markup *current-markup*))))

(defn render-special [name args]
  (case name
    "surrogate" (let [obj (first args)
                      converted-args (map render-json-ml (rest args))]
                  (apply make-surrogate (concat [obj] converted-args)))
    "reference" (apply make-reference (map render-json-ml args))
    (assert-markup-error (str "no matching special tag name: '" name "'"))))

(defn render-sub [tag children]
  (let [[html-tag style] tag]
    (apply make-template html-tag style (map render-json-ml (keep pref children)))))

(defn render-json-ml* [markup]
  (if-not (sequential? markup)
    markup
    (let [tag (pref (first markup))]
      (cond
        (string? tag) (render-special tag (rest markup))
        (sequential? tag) (render-sub tag (rest markup))
        :else (assert-markup-error (str "invalid json-ml markup at\n " (pprint-markup markup) "\n"))))))

(defn render-json-ml [markup]
  (set! *current-markup* markup)
  (let [res (render-json-ml* markup)]
    (set! *current-markup* nil)
    res))

