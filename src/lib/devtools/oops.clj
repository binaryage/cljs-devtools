(ns devtools.oops)

; these are private utilities, if you interested in similar funtionality please consider using cljs-oops library

(defmacro ocall [o name & params]
  `(let [o# ~o]
     (.call (cljs.core/aget o# ~name) o# ~@params)))

(defmacro oapply [o name param-coll]
  `(let [o# ~o]
     (.apply (cljs.core/aget o# ~name) o# (into-array ~param-coll))))

(defmacro oget
  ([o k] `(cljs.core/aget ~o ~k))
  ([o k & ks] `(if-let [o# (cljs.core/aget ~o ~k)]
                 (oget o# ~@ks))))

(defmacro oset [o ks val]
  (let [keys (butlast ks)
        obj-sym (gensym)]
    `(let [~obj-sym ~o
           target# ~(if (seq keys) `(oget ~obj-sym ~@keys) obj-sym)]
       (assert target# (str "unable to locate object path " ~keys " in " ~obj-sym))
       (cljs.core/aset target# ~(last ks) ~val)
       ~obj-sym)))

(defmacro safe-call [f exceptional-result & args]
  `(try
     (~f ~@args)
     (catch :default _e#
       ~exceptional-result)))
