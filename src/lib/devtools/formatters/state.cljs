(ns devtools.formatters.state)

; - state management --------------------------------------------------------------------------------------------------------
;
; we have to maintain some state:
; a) to prevent infinite recursion in some pathological cases (https://github.com/binaryage/cljs-devtools/issues/2)
; b) to keep track of printed objects to visually signal circular data structures
;
; We dynamically bind *current-config* to the config passed from "outside" when entering calls to our API methods.
; Initially the state is empty, but we accumulate there a history of seen values when rendering individual values
; in depth-first traversal order. See alt-printer-impl where we re-bind *current-config* for each traversal level.
; But there is a catch. For larger data structures our printing methods usually do not print everything at once.
; We can include so called "object references" which are just placeholders which can be expanded later
; by DevTools UI (when user clicks a disclosure triangle).
; For proper continuation in rendering of those references we have to carry our existing state over.
; We use "config" feature of custom formatters system to pass current state to future API calls.

(def ^:dynamic *current-state* nil)

(defn valid-current-state? []
  (some? *current-state*))

(defn get-default-state []
  {})

(defn get-current-state []
  {:pre [(valid-current-state?)]}
  *current-state*)

(defn update-current-state! [f & args]
  {:pre [(valid-current-state?)]}
  (set! *current-state* (apply f *current-state* args)))

; -- high level API ---------------------------------------------------------------------------------------------------------

(defn push-object-to-current-history! [object]
  (update-current-state! update :history conj object))

(defn get-current-history []
  (:history (get-current-state)))

(defn is-circular? [object]
  (let [history (get-current-history)]
    (some #(identical? % object) history)))

(defn ^bool prevent-recursion? []
  (boolean (:prevent-recursion (get-current-state))))

(defn set-prevent-recursion [state val]
  (if (some? val)
    (assoc state :prevent-recursion val)
    (dissoc state :prevent-recursion)))

(defn get-managed-print-level []
  (:managed-print-level (get-current-state)))

(defn set-managed-print-level [state val]
  (if (some? val)
    (assoc state :managed-print-level val)
    (dissoc state :managed-print-level)))

(defn get-depth-budget []
  (:depth-budget (get-current-state)))

(defn set-depth-budget [state val]
  (if (some? val)
    (assoc state :depth-budget val)
    (dissoc state :depth-budget)))

(defn reset-depth-limits [state]
  (-> state
      (set-depth-budget nil)
      (set-managed-print-level nil)))
