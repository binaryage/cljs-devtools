(ns devtools.oops
  "These are private utilities, if you interested in similar funtionality please consider using cljs-oops library.")

; -- code generators --------------------------------------------------------------------------------------------------------

(defn gen-ocall [o name params]
  `(let [o# ~o]
     (.call (cljs.core/aget o# ~name) o# ~@params)))

(defn gen-oapply [o name param-coll]
  `(let [o# ~o]
     (.apply (cljs.core/aget o# ~name) o# (into-array ~param-coll))))

(defn gen-oget
  ([o k] `(cljs.core/aget ~o ~k))
  ([o k & ks] (let [o-sym (gensym "o")]
                `(if-let [~o-sym (cljs.core/aget ~o ~k)]
                   ~(apply gen-oget o-sym ks)))))

(defn gen-oset [o ks val]
  (let [keys (butlast ks)
        obj-sym (gensym)]
    `(let [~obj-sym ~o
           target# ~(if (seq keys) `(oget ~obj-sym ~@keys) obj-sym)]
       (assert target# (str "unable to locate object path " ~keys " in " ~obj-sym))
       (cljs.core/aset target# ~(last ks) ~val)
       ~obj-sym)))

(defn gen-safe-call [f exceptional-result args]
  `(try
     (~f ~@args)
     (catch :default _e#
       ~exceptional-result)))

; -- macro wrappers ---------------------------------------------------------------------------------------------------------

(defmacro ocall [o name & params]
  (gen-ocall o name params))

(defmacro oapply [o name params]
  (gen-oapply o name params))

(defmacro oget [o k & ks]
  (apply gen-oget o k ks))

(defmacro oset [o ks val]
  (gen-oset o ks val))

(defmacro safe-call [f exceptional-result & args]
  (gen-safe-call f exceptional-result args))
