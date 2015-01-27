(ns devtools.core)

(defn build-header [value]
  #js ["span" #js {"style" "background-color: #cfc"}, (pr-str value)])

; dirty
(defn cljs-value? [value]
  (exists? (aget value "meta")))

(defn js-value? [value]
  (not (cljs-value? value)))

(defn header-hook [value]
  (if (cljs-value? value)
    (build-header value)
    nil
  )
)

(defn has-body-hook [value]
  false
  )

(defn body-hook [value]
  )

(def cljs-formatter (js-obj
                     "header" header-hook
                     "hasBody" has-body-hook
                     "body" body-hook))

(defn support-devtools! []
  (aset js/window "devtoolsFormatter" cljs-formatter))

(defn unsupport-devtools! []
  (aset js/window "devtoolsFormatter" nil))
