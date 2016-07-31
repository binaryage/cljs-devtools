(ns devtools.formatters.budgeting
  (:require-macros [devtools.util :refer [oget oset ocall oapply safe-call]])
  (:require [devtools.formatters.templating :refer [render-markup]]
            [devtools.formatters.state :refer [get-depth-budget set-depth-budget]]
            [devtools.formatters.helpers :refer [pref]]
            [devtools.formatters.markup :refer [<header-expander>]]))

; This functionality provides a workaround to issue #22 (https://github.com/binaryage/cljs-devtools/issues/22).
; The idea is to track hierarchy depth for json-ml(s) we are generating.
; If we are about to cross the depth limit hardcoded in WebKit,
; instead we render simple expandable placeholders which resume full rendering in their bodies (when expanded by user).
; Note that this technique has some quirks, it may break styling in some pathological cases.

; we need to reserve some depth levels for our expander symbol markup
(def header-expander-depth-cost 2)

; -- tracking over-budget values  -------------------------------------------------------------------------------------------

; note: phantomjs does not have WeakSet, so we have to emulate it when testing
(def over-budget-values (if (exists? js/WeakSet) (js/WeakSet.) (volatile! #{})))

(defn add-over-budget-value! [value]
  (if (volatile? over-budget-values)
    (vreset! over-budget-values (conj @over-budget-values value))
    (ocall over-budget-values "add" value)))

(defn delete-over-budget-value! [value]
  (if (volatile? over-budget-values)
    (vreset! over-budget-values (disj @over-budget-values value))
    (ocall over-budget-values "delete" value)))

(defn has-over-budget-value? [value]
  (if (volatile? over-budget-values)
    (contains? @over-budget-values value)
    (ocall over-budget-values "has" value)))

; -- depth budget accounting ------------------------------------------------------------------------------------------------

(defn object-reference? [json-ml]
  (= (first json-ml) "object"))

(defn determine-depth [json-ml]
  (if (array? json-ml)
    (inc (apply max (map determine-depth json-ml)))
    0))

(defn has-any-object-reference? [json-ml]
  (if (array? json-ml)
    (if (object-reference? json-ml)
      true
      (some has-any-object-reference? json-ml))))

(defn transfer-remaining-depth-budget! [object-reference depth-budget]
  {:pre [(not (neg? depth-budget))]}
  (let [data (second object-reference)
        _ (assert (object? data))
        config (oget data "config")]
    (oset data ["config"] (set-depth-budget config depth-budget))))

(defn distribute-budget! [json-ml depth-budget]
  {:pre [(not (neg? depth-budget))]}
  (if (array? json-ml)
    (let [new-depth-budget (dec depth-budget)]
      (if (object-reference? json-ml)
        (transfer-remaining-depth-budget! json-ml new-depth-budget)
        (doseq [item json-ml]
          (distribute-budget! item new-depth-budget)))))
  json-ml)

; -- api --------------------------------------------------------------------------------------------------------------------

(defn was-over-budget?! [value]
  (when (has-over-budget-value? value)
    (delete-over-budget-value! value)
    true))

(defn alter-json-ml-to-fit-in-remaining-budget! [value json-ml]
  (if-let [initial-hierarchy-depth-budget (pref :initial-hierarchy-depth-budget)]                                             ; this is hardcoded in InjectedScriptSource.js in WebKit, look for maxCustomPreviewRecursionDepth
    (let [remaining-depth-budget (or (get-depth-budget) (dec initial-hierarchy-depth-budget))
          depth (determine-depth json-ml)
          final? (not (has-any-object-reference? json-ml))
          needed-depth (if final? depth (+ depth header-expander-depth-cost))]
      (if (>= remaining-depth-budget needed-depth)
        (distribute-budget! json-ml remaining-depth-budget)
        (let [expander-ml (render-markup (<header-expander> value))]
          (add-over-budget-value! value)                                                                                      ; we need to record over-budget values to for later was-over-budget?! check, see has-body* in formatters.core
          expander-ml)))
    json-ml))
