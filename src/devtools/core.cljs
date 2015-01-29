(ns devtools.core)

(def MAX_COLLECTION_ELEMENTS 10)
(def MAX_MAP_ELEMENTS 5)
(def MAX_SET_ELEMENTS 7)

(declare header-template)
(declare container-value-template)

; dirty
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "_hash"))))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn template [tag style & more]
  (let [arr #js [tag #js {"style" style}]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn reference [object & more]
  (let [arr #js ["object" #js {"object" object}]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn spacer [& _] " ")

(defn nil-template [_]
  (template "span" "color: #808080" "nil"))

(defn keyword-template [v]
  (template "span" "color: #881391" (str ":" (name v))))

(defn symbol-template [v]
  (template "span" "color: #881391" (str v)))

(defn number-template [v]
  (if (integer? v)
    (template "span" "color: #1C00CF" v)
    (template "span" "color: #1C88CF" v)))

(defn fn-template [v]
  (template "span" "color: #f00" (reference v) "fn"))

(defn string-template [v]
  (template "span" "color: #C41A16" (str "\"" v "\"")))

(defn header-collection-template [v]
  (let [arr (template "span" "" "[")]
    (doseq [x (take MAX_COLLECTION_ELEMENTS v)]
      (.push arr (container-value-template x) (spacer x)))
    (.pop arr)
    (if (> (count v) MAX_COLLECTION_ELEMENTS)
      (.push arr "…"))
    (.push arr "]")
    arr))

(defn header-map-template [m]
  (let [arr (template "span" "" "{")
        v (seq m)]
    (doseq [[k v] (take MAX_MAP_ELEMENTS v)]
      (.push arr
             (container-value-template k) (spacer k)
             (container-value-template v) (spacer v)))
    (.pop arr)
    (if (> (count v) MAX_MAP_ELEMENTS)
      (.push arr "…"))
    (.push arr "}")
    arr))

(defn header-set-template [v]
  (let [arr (template "span" "" "#{")]
    (doseq [x (take MAX_SET_ELEMENTS v)]
      (.push arr (container-value-template x) (spacer x)))
    (.pop arr)
    (if (> (count v) MAX_SET_ELEMENTS)
      (.push arr "…"))
    (.push arr "}")
    arr))

(defn generic-template [v]
  (template "span" "" (reference v) "Object"))

(defn atomic-template [x]
  (cond
    (nil? x) (nil-template x)
    (string? x) (string-template x)
    (number? x) (number-template x)
    (keyword? x) (keyword-template x)
    (symbol? x) (symbol-template x)
    (fn? x) (fn-template x)
    ))

(defn container-template [x]
  (cond
    (map? x) (header-map-template x)
    (set? x) (header-set-template x)
    (coll? x) (header-collection-template x)
    ))

(defn container-value-template [x]
  (or (atomic-template x)
      (generic-template x)))

(defn header-template [x]
  (or (atomic-template x)
      (container-template x)
      (pr-str x)))

(defn build-header [value]
  (template "span" "background-color: #efe" (header-template value)))

(defn header-hook [value]
  (if (cljs-value? value)
    (build-header value)))

(defn has-body-hook [value]
  false)

(defn body-hook [value])

(def cljs-formatter
  (js-obj
     "header" header-hook
     "hasBody" has-body-hook
     "body" body-hook))

(defn support-devtools! []
  (aset js/window "devtoolsFormatter" cljs-formatter))

(defn unsupport-devtools! []
  (aset js/window "devtoolsFormatter" nil))
