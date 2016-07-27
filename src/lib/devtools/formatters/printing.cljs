(ns devtools.formatters.printing
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.format :refer [IDevtoolsFormat]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.state :refer [push-object-to-current-history! *current-state* get-current-state
                                               is-circular?]]
            [devtools.formatters.helpers :refer [cljs-value? expandable? abbreviated?]]))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn markup? [value]
  (::markup (meta value)))

(defn mark-as-markup [value]
  (with-meta value {::markup true}))

(defn wrap-value-as-reference-if-needed [value]
  (if (and (cljs-value? value) (not (markup? value)))
    ["reference" value]
    value))

(defn build-markup [markup-fns fn-key & args]
  (let [f (get markup-fns fn-key)]
    (assert f (str "missing markup method in opts: " fn-key))
    (mark-as-markup (apply f args))))

; -- TemplateWriter ---------------------------------------------------------------------------------------------------------

(deftype TemplateWriter [^:mutable group]
  Object
  (merge [_ a] (set! group (concat group a)))
  (get-group [_] group)
  IWriter
  (-write [_ o] (set! group (concat group [(wrap-value-as-reference-if-needed o)])))                                          ; issue #21
  (-flush [_] nil))

(defn make-template-writer []
  (TemplateWriter. []))

; -- post-processing --------------------------------------------------------------------------------------------------------

(defn already-reference? [group]
  (if-let [tag (first (first group))]
    (= tag "reference")))

(defn wrap-group-in-reference-if-needed [group obj markup]
  (if (and (not (already-reference? group))
           (or (expandable? obj) (abbreviated? group)))
    (let [expandable-markup (apply build-markup markup :expandable group)
          surrogate-markup (build-markup markup :raw-surrogate obj expandable-markup :target)
          reference-markup (build-markup markup :reference surrogate-markup)]
      [reference-markup])
    group))

(defn wrap-group-in-circular-warning-if-needed [group markup circular?]
  (if circular?
    [(apply build-markup markup :circular-reference group)]
    group))

(defn wrap-group-in-meta-if-needed [group value markup]
  (if-let [meta-data (if (pref :print-meta-data) (meta value))]
    [(apply (partial (:meta-wrapper markup) meta-data) group)]
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
    [(build-markup markup :native-reference obj)]

    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
    [(aget group 0) (build-markup :native-reference obj) (aget group 2)]

    :else group))

(defn post-process-printed-output [output-group obj markup circular?]
  (-> output-group
      (detect-edge-case-and-patch-it obj markup)                                                                              ; an ugly hack
      (wrap-group-in-reference-if-needed obj markup)
      (wrap-group-in-circular-warning-if-needed markup circular?)
      (wrap-group-in-meta-if-needed obj markup)))

; -- alternative printer ----------------------------------------------------------------------------------------------------

(defn alt-printer-job [obj writer opts]
  (let [{:keys [markup-fns]} opts]
    (if (or (safe-call satisfies? false IDevtoolsFormat obj)
            (safe-call satisfies? false IFormat obj))                                                                         ; we have to wrap value in reference if detected IFormat
      (-write writer (build-markup markup-fns :reference obj))
      (if-let [atomic-markup (build-markup markup-fns :atomic obj)]
        (-write writer atomic-markup)
        (let [default-impl (:fallback-impl opts)
              ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
              inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
          (default-impl obj writer inner-opts))))))

(defn alt-printer-impl [obj writer opts]
  (binding [*current-state* (get-current-state)]
    (let [{:keys [markup-fns]} opts
          circular? (is-circular? obj)
          inner-writer (make-template-writer)]
      (push-object-to-current-history! obj)
      (alt-printer-job obj inner-writer opts)
      (.merge writer (post-process-printed-output (.get-group inner-writer) obj markup-fns circular?)))))

; -- common code for managed printing ---------------------------------------------------------------------------------------

(defn managed-print [tag markup-fns printer]
  (let [writer (make-template-writer)
        opts {:alt-impl     alt-printer-impl
              :markup-fns   markup-fns
              :print-length (pref :max-header-elements)
              :more-marker  (pref :more-marker)}]
    (printer writer opts)
    (concat [(pref tag)] (.get-group writer))))

; -- public printing API ----------------------------------------------------------------------------------------------------

(defn managed-print-via-writer [value tag markup-fns]
  (managed-print tag markup-fns (fn [writer opts]
                                  (pr-seq-writer [value] writer opts))))                                                      ; note we use pr-seq-writer becasue pr-writer is private for some reason

(defn managed-print-via-protocol [value tag markup-fns]
  (managed-print tag markup-fns (fn [writer opts]
                                  (-pr-writer value writer opts))))
