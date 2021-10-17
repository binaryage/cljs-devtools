(ns devtools.formatters.templating
  (:require-macros [devtools.oops :refer [oget oset ocall oapply safe-call unchecked-aget]])
  (:require [clojure.walk :refer [prewalk]]
            [devtools.util :refer [pprint-str]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.helpers :refer [pref cljs-value?]]
            [devtools.formatters.state :refer [get-current-state prevent-recursion?]]
            [clojure.string :as string]))

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
       (= (unchecked-aget value 0) "object")))

; ---------------------------------------------------------------------------------------------------------------------------

(defn make-group [& items]
  (let [group (mark-as-group! #js [])]
    (doseq [item items]
      (if (some? item)
        (if (coll? item)
          (.apply (unchecked-aget group "push") group (mark-as-group! (into-array item)))                                               ; convenience helper to splat cljs collections
          (.push group (pref item)))))
    group))

(defn make-template
  [tag style & children]
  (let [tag (pref tag)
        style (pref style)
        template (mark-as-template! #js [tag (if (empty? style)
                                               #js {}
                                               #js {"style" style})])]
    (doseq [child children]
      (if (some? child)
        (if (coll? child)
          (.apply (unchecked-aget template "push") template (mark-as-template! (into-array (keep pref child))))                         ; convenience helper to splat cljs collections
          (if-let [child-value (pref child)]
            (.push template child-value)))))
    template))

(defn concat-templates! [template & templates]
  (mark-as-template! (.apply (oget template "concat") template (into-array (map into-array (keep pref templates))))))

(defn extend-template! [template & args]
  (concat-templates! template args))

(defn make-surrogate
  ; passing :target as body means that targt object body should be rendered using standard templates
  ; see <surrogate-body> in markup.cljs
  ([object] (make-surrogate object nil))
  ([object header] (make-surrogate object header nil))
  ([object header body] (make-surrogate object header body 0))
  ([object header body start-index]
   (mark-as-surrogate! (js-obj
                         "target" object
                         "header" header
                         "body" body
                         "startIndex" (or start-index 0)))))

(defn get-surrogate-target [surrogate]
  {:pre [(surrogate? surrogate)]}
  (oget surrogate "target"))

(defn get-surrogate-header [surrogate]
  {:pre [(surrogate? surrogate)]}
  (oget surrogate "header"))

(defn get-surrogate-body [surrogate]
  {:pre [(surrogate? surrogate)]}
  (oget surrogate "body"))

(defn get-surrogate-start-index [surrogate]
  {:pre [(surrogate? surrogate)]}
  (oget surrogate "startIndex"))

(defn make-reference [object & [state-override-fn]]
  {:pre [(or (nil? state-override-fn) (fn? state-override-fn))]}
  (if (nil? object)
    ; this code is duplicated in markup.cljs <nil>
    (make-template :span :nil-style :nil-label)
    (let [sub-state (if (some? state-override-fn)
                      (state-override-fn (get-current-state))
                      (get-current-state))]
      (make-group "object" #js {"object" object
                                "config" sub-state}))))

(defn make-annotation [data markups]
  (apply make-group "annotation" (clj->js data) markups))

; -- JSON ML support --------------------------------------------------------------------------------------------------------

; a renderer from hiccup-like data markup to json-ml
;
; [[tag style] child1 child2 ...] -> #js [tag #js {"style" ...} child1 child2 ...]
;

(declare render-json-ml*)

(def ^:dynamic *current-render-stack* [])
(def ^:dynamic *current-render-path* [])

(defn print-preview [markup]
  (binding [*print-level* 1]
    (pr-str markup)))

(defn add-stack-separators [stack]
  (interpose "-------------" stack))

(defn replace-fns-with-markers [stack]
  (let [f (fn [v]
            (if (fn? v)
              "##fn##"
              v))]
    (prewalk f stack)))

(defn pprint-render-calls [stack]
  (map pprint-str stack))

(defn pprint-render-stack [stack]
  (string/join "\n" (-> stack
                        reverse
                        replace-fns-with-markers
                        pprint-render-calls
                        add-stack-separators)))

(defn pprint-render-path [path]
  (pprint-str path))

(defn assert-markup-error [msg]
  (assert false (str msg "\n"
                     "Render path: " (pprint-render-path *current-render-path*) "\n"
                     "Render stack:\n"
                     (pprint-render-stack *current-render-stack*))))

(defn surrogate-markup? [markup]
  (and (sequential? markup) (= (first markup) "surrogate")))

(defn render-special [name args]
  (case name
    "surrogate" (let [obj (first args)
                      converted-args (map render-json-ml* (rest args))]
                  (apply make-surrogate (concat [obj] converted-args)))
    "reference" (let [obj (first args)
                      converted-obj (if (surrogate-markup? obj) (render-json-ml* obj) obj)]
                  (apply make-reference (concat [converted-obj] (rest args))))
    "annotation" (let [data (first args)
                       converted-args (map render-json-ml* (rest args))]
                   (make-annotation data converted-args))
    (assert-markup-error (str "no matching special tag name: '" name "'"))))

(defn emptyish? [v]
  (if (or (seqable? v) (array? v) (string? v))
    (empty? v)
    false))

(defn render-subtree [tag children]
  (let [[html-tag style] tag]
    (apply make-template html-tag style (map render-json-ml* (remove emptyish? (map pref children))))))

(defn render-json-ml* [markup]
  (if-not (sequential? markup)
    markup
    (binding [*current-render-path* (conj *current-render-path* (first markup))]
      (let [tag (pref (first markup))]
        (cond
          (string? tag) (render-special tag (rest markup))
          (sequential? tag) (render-subtree tag (rest markup))
          :else (assert-markup-error (str "invalid json-ml markup at " (print-preview markup) ":")))))))

(defn render-json-ml [markup]
  (binding [*current-render-stack* (conj *current-render-stack* markup)
            *current-render-path* (conj *current-render-path* "<render-json-ml>")]
    (render-json-ml* markup)))

; -- template rendering -----------------------------------------------------------------------------------------------------

(defn ^:dynamic assert-failed-markup-rendering [initial-value value]
  (assert false (str "result of markup rendering must be a template,\n"
                     "resolved to " (pprint-str value)
                     "initial value: " (pprint-str initial-value))))

(defn render-markup* [initial-value value]
  (cond
    (fn? value) (recur initial-value (value))
    (keyword? value) (recur initial-value (pref value))
    (sequential? value) (recur initial-value (render-json-ml value))
    (template? value) value
    (surrogate? value) value
    (reference? value) value
    :else (assert-failed-markup-rendering initial-value value)))

(defn render-markup [value]
  (render-markup* value value))
