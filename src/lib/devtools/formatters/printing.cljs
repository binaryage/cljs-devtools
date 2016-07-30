(ns devtools.formatters.printing
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.prefs :refer [pref]]
            [devtools.format :refer [IDevtoolsFormat]]
            [devtools.protocols :refer [ITemplate IGroup ISurrogate IFormat]]
            [devtools.formatters.state :refer [push-object-to-current-history! *current-state* get-current-state
                                               is-circular? get-managed-print-level set-managed-print-level
                                               update-current-state!]]
            [devtools.formatters.helpers :refer [cljs-value? expandable? abbreviated? directly-printable?]]))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn markup? [value]
  (::markup (meta value)))

(defn mark-as-markup [value]
  (with-meta value {::markup true}))

(defn build-markup [markup-db fn-key & args]
  (let [f (get markup-db fn-key)]
    (assert f (str "missing markup method in markup-db: " fn-key))
    (mark-as-markup (apply f args))))

(defn wrap-value-as-reference-if-needed [markup-db value]
  (if (or (directly-printable? value) (markup? value))
    value
    (build-markup markup-db :reference-surrogate value)))

; -- TemplateWriter ---------------------------------------------------------------------------------------------------------

(deftype TemplateWriter [^:mutable group markup-db]
  Object
  (merge [_ a] (set! group (concat group a)))
  (get-group [_] group)
  IWriter
  (-write [_ o] (set! group (concat group [(wrap-value-as-reference-if-needed markup-db o)])))                                ; issue #21
  (-flush [_] nil))

(defn make-template-writer [markup-db]
  (TemplateWriter. [] markup-db))

; -- post-processing --------------------------------------------------------------------------------------------------------

(defn already-reference? [group]
  (if-let [tag (first (first group))]
    (= tag "reference")))

(defn wrap-group-in-reference-if-needed [group obj markup-db]
  (if (and (not (already-reference? group))
           (or (expandable? obj) (abbreviated? group)))
    (let [expandable-markup (apply build-markup markup-db :expandable group)
          surrogate-markup (build-markup markup-db :raw-surrogate obj expandable-markup :target)
          reference-markup (build-markup markup-db :reference surrogate-markup)]
      [reference-markup])
    group))

(defn wrap-group-in-circular-warning-if-needed [group markup-db circular?]
  (if circular?
    [(apply build-markup markup-db :circular-reference group)]
    group))

(defn wrap-group-in-meta-if-needed [group value markup-db]
  (if-let [meta-data (if (pref :print-meta-data) (meta value))]
    [(apply (partial (:meta-wrapper markup-db) meta-data) group)]
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
(defn detect-edge-case-and-patch-it [group obj markup-db]
  (cond
    (or
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "\"]"))                                        ; function case
      (and (= (count group) 5) (= (aget group 0) "#object[") (= (aget group 4) "]"))                                          ; :else -constructor case
      (and (= (count group) 3) (= (aget group 0) "#object[") (= (aget group 2) "]")))                                         ; :else -cljs$lang$ctorStr case
    [(build-markup markup-db :native-reference obj)]

    (and (= (count group) 3) (= (aget group 0) "#<") (= (str obj) (aget group 1)) (= (aget group 2) ">"))                     ; old code prior r1.7.28
    [(aget group 0) (build-markup :native-reference obj) (aget group 2)]

    :else group))

(defn post-process-printed-output [output-group obj markup-db circular?]
  (-> output-group
      (detect-edge-case-and-patch-it obj markup-db)                                                                           ; an ugly hack
      (wrap-group-in-reference-if-needed obj markup-db)
      (wrap-group-in-circular-warning-if-needed markup-db circular?)
      (wrap-group-in-meta-if-needed obj markup-db)))

; -- alternative printer ----------------------------------------------------------------------------------------------------

(defn alt-printer-job [obj writer opts]
  (let [{:keys [markup-db]} opts]
    (if (or (safe-call satisfies? false IDevtoolsFormat obj)
            (safe-call satisfies? false IFormat obj))                                                                         ; we have to wrap value in reference if detected IFormat
      (-write writer (build-markup markup-db :reference obj))
      (if-let [atomic-markup (build-markup markup-db :atomic obj)]
        (-write writer atomic-markup)
        (let [default-impl (:fallback-impl opts)
              ; we want to limit print-level, at max-print-level level use maximal abbreviation e.g. [...] or {...}
              inner-opts (if (= *print-level* 1) (assoc opts :print-length 0) opts)]
          (default-impl obj writer inner-opts))))))

(defn alt-printer-impl [obj writer opts]
  (binding [*current-state* (get-current-state)]
    (let [{:keys [markup-db]} opts
          circular? (is-circular? obj)
          inner-writer (make-template-writer (:markup-db opts))]
      (push-object-to-current-history! obj)
      (alt-printer-job obj inner-writer opts)
      (.merge writer (post-process-printed-output (.get-group inner-writer) obj markup-db circular?)))))

; -- common code for managed printing ---------------------------------------------------------------------------------------

(defn managed-print [tag markup-db printer]
  (let [writer (make-template-writer markup-db)
        opts {:alt-impl     alt-printer-impl
              :markup-db    markup-db
              :print-length (pref :max-header-elements)
              :more-marker  (pref :more-marker)}
        job-fn #(printer writer opts)]
    (if-let [managed-print-level (get-managed-print-level)]
      (binding [*print-level* managed-print-level]
        (update-current-state! #(set-managed-print-level % nil))                                                              ; reset managed-print-level so it does not propagate further down in expaded data
        (job-fn))
      (job-fn))
    (concat [(pref tag)] (.get-group writer))))

; -- public printing API ----------------------------------------------------------------------------------------------------

(defn managed-print-via-writer [value tag markup-db]
  (managed-print tag markup-db (fn [writer opts]
                                 (pr-seq-writer [value] writer opts))))                                                       ; note we use pr-seq-writer becasue pr-writer is private for some reason

(defn managed-print-via-protocol [value tag markup-db]
  (managed-print tag markup-db (fn [writer opts]
                                 (-pr-writer value writer opts))))
