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

(defn get-last-object-from-current-history []
  (first (get-current-history)))                                                                                              ; note the list is reversed

(defn get-second-last-object-from-current-history []
  (second (get-current-history)))                                                                                              ; note the list is reversed

(defn present-path-segment [v]
  (cond
    (string? v) v
    ;; we'd like to preserve keywords for easy get
    (keyword? v) v
    (number? v) v
    :else "?"))

(defn seek-path-segment [coll val]
  (let [* (fn [[k v]]
            (cond
              ;; we need to know the paths for keywords, these are clickable
              (identical? k val)
              (present-path-segment k)

              (identical? v val)
              (present-path-segment k)))]
    (some * coll)))

(defn build-path-segment [parent-object object]
  (cond
    (map? parent-object) (seek-path-segment (seq parent-object) object)
    (sequential? parent-object) (seek-path-segment (map-indexed (fn [i x] [i x]) parent-object) object)))

;; This function checks a unique situation of looping an immediate child element `obj` of a parent element `history`
;; say we have a general map {:a 2 :b {:gh 45} :c 4}
;; and we call devtools.formatters.core/body-api-call with the map, the map ends up in
;; devtools.formatters.markup/<details> which then calls devtools.formatters.markup/body-lines
;; where the map will get seq'd resulting in ([:a 2] [:b {:gh 45}] [:c 4])
;; these 3 vectors will then be pushed to history which will result in an issue when generating the path
;; for example if we are looping over at `obj` as 2 and `history` as `[:a 2]` `build-path-segment` will return
;; the path as 1 since the immediate history is a vector instead of a map.
;; This function detects the condition that this is the case and then the next operation will be to
;; get the first item in the vector which is the path.
(defn mapping?
  [history obj]
  (let [first-kw (when (and (vector? obj)
                            (map? history))
                   (nth obj 0 nil))
        valid-kw? (and first-kw
                       (or (keyword? first-kw)
                           (string? first-kw)
                           (number? first-kw))
                       ;; intentionally delaying realizing the whole vector
                       (= (count obj) 2))]
    (when valid-kw?
      (contains? history first-kw))))

(defn ignore-path-in-fake-vector
  [history obj path]
  ;; if the current item we are looping at is an artificial vector (explained at `mapping` above),
  ;; don't append to the path
  (when (mapping? history obj)
    (or path [])))

(defn find-path-in-fake-vector
  [history path]
  (let [second-last-history (get-second-last-object-from-current-history)]
    ;; if the previous item is an artificial vector, lets append to the path info but take the first item
    ;; in the artificial vector as the path. (Explained in `mapping` above)
    (when (mapping? second-last-history history)
      (conj (or path []) (nth history 0 nil)))))

(defn find-path
  [history obj path]
  (let [path-segment (build-path-segment history obj)]
    (when (some? path-segment)
      (conj (or path []) path-segment))))

(defn extend-path-info [path-info object]
  (let [parent-object (get-last-object-from-current-history)]
    (or (ignore-path-in-fake-vector parent-object object path-info)
        (find-path-in-fake-vector parent-object path-info)
        (find-path parent-object object path-info)
        path-info)))

(defn add-object-to-current-path-info! [object]
  (update-current-state! update :path-info extend-path-info object))

(defn get-current-path-info []
  (:path-info (get-current-state)))

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
