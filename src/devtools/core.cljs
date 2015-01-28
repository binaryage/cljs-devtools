(ns devtools.core)

(def MAX_COLLECTION_ELEMENTS 10)
(def MAX_MAP_ELEMENTS 5)
(def MAX_SET_ELEMENTS 7)

(declare render-header-template)
(declare render-inline-header-template)
(declare render-embedded-header-value)

; dirty
(defn cljs-value? [value]
  (or (exists? (aget value "meta"))
      (exists? (aget value "_meta"))
      (exists? (aget value "_hash"))))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn build-template [tag style & more]
   (let [arr #js [tag #js {"style" style}]]
      (doseq [o more]
        (.push arr o))
     arr))

(defn build-placeholder [object & more]
  (let [arr #js ["object" #js {"object" object}]]
    (doseq [o more]
      (.push arr o))
    arr))

(defn spacer-template [x]
  " ")

(defn nil-template [v]
  (build-template "span" "color: #808080" "nil"))

(defn keyword-template [v]
  (build-template "span" "color: #881391" (str ":" (name v))))

(defn symbol-template [v]
  (build-template "span" "color: #881391" (str v)))

(defn integer-template [v]
  (build-template "span" "color: #1C00CF" v))

(defn fn-template [v]
  (build-template "span" "color: #f00" (build-placeholder v) "fn"))

(defn string-template [v]
  (build-template "span" "color: #C41A16" (str "\"" v "\"")))

(defn generic-template [v]
  (build-template "span" "" (build-placeholder v) "Object"))

(defn header-collection-template [v]
  (let [arr (build-template "span" "" "[")]
    (doseq [x (take MAX_COLLECTION_ELEMENTS v)]
      (.push arr (render-embedded-header-value x) (spacer-template x)))
    (.pop arr)
    (if (> (count v) MAX_COLLECTION_ELEMENTS)
      (.push arr "…"))
    (.push arr "]")
    arr))

(defn header-map-template [m]
  (let [arr (build-template "span" "" "{")
        v (seq m)]
    (doseq [[k v] (take MAX_MAP_ELEMENTS v)]
      (.push arr
             (render-embedded-header-value k) (spacer-template k)
             (render-embedded-header-value v) (spacer-template v)))
    (.pop arr)
    (if (> (count v) MAX_MAP_ELEMENTS)
      (.push arr "…"))
    (.push arr "}")
    arr))

(defn header-set-template [v]
  (let [arr (build-template "span" "" "#{")]
    (doseq [x (take MAX_SET_ELEMENTS v)]
      (.push arr (render-embedded-header-value x) (spacer-template x)))
    (.pop arr)
    (if (> (count v) MAX_SET_ELEMENTS)
      (.push arr "…"))
    (.push arr "}")
    arr))

(defn render-atomic-template [x]
  (cond
    (nil? x) (nil-template x)
    (string? x) (string-template x)
    (integer? x) (integer-template x)
    (keyword? x) (keyword-template x)
    (symbol? x) (symbol-template x)
    (fn? x) (fn-template x)
    ))

(defn render-embedded-header-value [x]
  (or (render-atomic-template x)
      (generic-template x)))

(defn render-header-template [x]
  (or (render-atomic-template x)
      (cond
        (nil? x) (nil-template x)
        (keyword? x) (keyword-template x)
        (symbol? x) (symbol-template x)
        (map? x) (header-map-template x)
        (set? x) (header-set-template x)
        (coll? x) (header-collection-template x)
        :else (pr-str x))))

(defn build-header [value]
  (build-template "span" "background-color: #eff" (render-header-template value)))

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
