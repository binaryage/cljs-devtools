(ns devtools.formatters.printing
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.format :refer [IDevtoolsFormat]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.templating :refer [make-template make-group render-json-ml concat-templates!
                                                    template? group? make-reference]]
            [devtools.formatters.state :refer [push-object-to-current-history! *current-state* get-current-state
                                               is-circular?]]
            [devtools.formatters.helpers :refer [cljs-value?]]))

(defn build-markup [markup-fns fn-key & args]
  (let [f (get markup-fns fn-key)]
    (assert f (str "missing markup method in opts: " fn-key))
    (apply f args)))

; -- TemplateWriter ---------------------------------------------------------------------------------------------------------

(defn wrap-value-as-reference-if-needed [value]
  (if (cljs-value? value)
    (make-reference value)
    value))

(deftype TemplateWriter [group]
  Object
  (merge [_ a] (.apply (.-push group) group a))
  (get-group [_] group)
  IWriter
  (-write [_ o] (.push group (wrap-value-as-reference-if-needed o)))
  (-flush [_] nil))


(defn make-template-writer []
  (TemplateWriter. (make-group)))

; -- TemplateWriter ---------------------------------------------------------------------------------------------------------

(defn abbreviated? [template]
  (some #(= (pref :more-marker) %) template))

(defn seq-count-is-greater-or-equal? [seq limit]
  (let [chunk (take limit seq)]                                                                                               ; we have to be extra careful to not call count on seq, it might be an infinite sequence
    (= (count chunk) limit)))

(defn expandable? [obj]
  (and
    (pref :seqables-always-expandable)
    (seqable? obj)
    (seq-count-is-greater-or-equal? obj (pref :min-sequable-count-for-expansion))))

(defn wrap-group-in-reference-if-needed [group obj markup]
  (if (or (expandable? obj) (abbreviated? group))
    (make-group (render-json-ml (build-markup markup :reference-surrogate obj (concat-templates! (make-template :span :header-style) group))))
    group))

(defn wrap-group-in-circular-warning-if-needed [group markup circular?]
  (if circular?
    (make-group (render-json-ml (apply build-markup markup :circular-reference (vec group))))
    group))

(defn wrap-group-in-meta-if-needed [group value markup]
  (if-let [meta-data (if (pref :print-meta-data) (meta value))]
    (make-group (render-json-ml (apply (partial (:meta-wrapper markup) meta-data) (vec group))))
    group))

; default printer implementation can do this:
;   :else (write-all writer "#<" (str obj) ">")
; we want to wrap stringified obj in a reference for further inspection
;
; this behaviour changed in https://github.com/clojure/clojurescript/commit/34c3b8985ed8197d90f441c46d168c4024a20eb8
; newly functions and :else branch print "#object [" ... "]"
;
; in some situations obj can still be a clojurescript value (e.g. deftypes)
; we have to implement a special flag to prevent infinite recursion
; see https://github.com/binaryage/cljs-devtools/issues/2
;     https://github.com/binaryage/cljs-devtools/issues/8
(defn detect-edge-case-and-patch-it [group obj markup]
  (cond
    (or
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "\"]"))                                        ; function case
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "]"))                                          ; :else -constructor case
      (and (= (count group) 3) (= (aget group 0) "#object[") (= (aget group 2) "]")))                                         ; :else -cljs$lang$ctorStr case
    (make-group (render-json-ml (build-markup markup :native-reference obj)))

    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
    (make-group (aget group 0) (render-json-ml (build-markup :native-reference obj)) (aget group 2))

    :else group))

;(defn wrap-value-as-reference-if-needed [value markup]
;  (if (or (string? value) (template? value) (group? value))
;    value
;    (render-json-ml (build-markup markup :reference value))))
;
;(defn wrap-group-values-as-references-if-needed [group markup]
;  (let [result (make-group)]
;    (doseq [value group]
;      (.push result (wrap-value-as-reference-if-needed value markup)))
;    result))

(defn alt-printer-job [obj writer opts]
  (let [{:keys [markup-fns]} opts]
    (if (or (safe-call satisfies? false IDevtoolsFormat obj)
            (safe-call satisfies? false IFormat obj))                                                                         ; we have to wrap value in reference if detected IFormat
      (-write writer (render-json-ml (build-markup markup-fns :reference obj)))
      (if-let [tmpl (build-markup markup-fns :atomic obj)]
        (-write writer (render-json-ml tmpl))
        (let [default-impl (:fallback-impl opts)
              ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
              inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
          (default-impl obj writer inner-opts))))))

(defn post-process-printed-output [output-group obj markup circular?]
  (-> output-group
      (detect-edge-case-and-patch-it obj markup)                                                                              ; an ugly hack
      ;(wrap-group-values-as-references-if-needed markup)                                                                      ; issue #21
      (wrap-group-in-reference-if-needed obj markup)
      (wrap-group-in-circular-warning-if-needed markup circular?)
      (wrap-group-in-meta-if-needed obj markup)))

(defn alt-printer-impl [obj writer opts]
  (binding [*current-state* (get-current-state)]
    (let [circular? (is-circular? obj)
          {:keys [markup-fns]} opts
          inner-writer (make-template-writer)]
      (push-object-to-current-history! obj)
      (alt-printer-job obj inner-writer opts)
      (.merge writer (post-process-printed-output (.get-group inner-writer) obj markup-fns circular?)))))

(defn managed-pr-str [value style print-level markup-fns]
  (let [tmpl (make-template :span style)
        writer (TemplateWriter. tmpl)]
    (binding [*print-level* (inc print-level)]                                                                                ; when printing do at most print-level deep recursion
      (pr-seq-writer [value] writer {:alt-impl     alt-printer-impl
                                     :markup-fns   markup-fns
                                     :print-length (pref :max-header-elements)
                                     :more-marker  (pref :more-marker)}))
    tmpl))
