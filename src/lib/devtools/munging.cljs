(ns devtools.munging
  (:require [clojure.string :as string]
            [devtools.util :refer-macros [oget oset ocall]]))

(def dollar-replacement "~﹩~")

(defn cljs-fn-name? [munged-name]
  (if (string? munged-name)
    (some? (re-matches #"^[^$]*\$[^$]+\$.*$" munged-name))))                                                                  ; must have at least two dollars because we assume two-segment namespaces

(defn parse-fn-source [fn-source]
  ; fn-source could look like this:
  ; "function devtools_sample$core$hello(name){
  ;    return [cljs.core.str("hello, "),cljs.core.str(name),cljs.core.str("!")].join('');
  ;  }"
  (if-let [meat (second (re-find #"function\s(.*?)\{" fn-source))]
    (if-let [[_ name args] (re-find #"(.*?)\((.*)\)" meat)]
      [name args])))

(defn get-fn-source-safe [f]
  (try
    (ocall f "toString")
    (catch :default _)))

(defn trivial-fn-source? [fn-source]
  (or (= fn-source "function () {}")
      (= fn-source "function Function() { [native code] }")))

(defn cljs-fn? [f]
  (let [name (oget f name)]
    (if-not (empty? name)
      (cljs-fn-name? name)
      (let [fn-source (get-fn-source-safe f)
            [name] (parse-fn-source fn-source)]
        (if-not (empty? name)
          (cljs-fn-name? name)
          (not (trivial-fn-source? fn-source)))))))                                                                           ; we assume non-trivial anonymous functions to come from cljs

(defn dollar-preserving-demunge [munged-name]
  (-> munged-name
      (string/replace "$" dollar-replacement)
      (demunge)
      (string/replace dollar-replacement "$")))

(defn demunge-ns [munged-name]
  (-> munged-name
      (dollar-preserving-demunge)
      (string/replace "$" ".")))

(defn ns-exists? [ns-module-name]
  {:pre [(string? ns-module-name)]}
  (if-let [goog-namespaces (oget js/window "goog" "dependencies_" "nameToPath")]
    (some? (oget goog-namespaces ns-module-name))))

(defn detect-namespace-prefix [parts]
  (loop [name-parts []
         remaining-parts parts]
    (if (empty? remaining-parts)
      ["" name-parts]
      (let [ns-name (string/join "." remaining-parts)]
        (if (ns-exists? ns-name)
          [ns-name name-parts]
          (recur (concat [(last remaining-parts)] name-parts) (butlast remaining-parts)))))))

(defn break-munged-name [munged-name]
  (let [parts (vec (.split munged-name "$"))
        [munged-ns name-parts] (detect-namespace-prefix parts)
        munged-name (string/join "$" name-parts)]
    [(demunge-ns munged-ns) (dollar-preserving-demunge munged-name)]))

(defn break-and-demunge-name [munged-name]
  (let [[munged-ns munged-name] (break-munged-name munged-name)]
    [(demunge-ns munged-ns) (dollar-preserving-demunge munged-name)]))

(defn parse-fn-source-info [fn-source]
  (if-let [[munged-name args] (parse-fn-source fn-source)]
    (let [[ns name] (break-and-demunge-name munged-name)
          demunged-args (map (comp dollar-preserving-demunge string/trim) (string/split args #","))]
      (concat [ns name] demunged-args))))

(defn parse-fn-info [f]
  (let [fn-source (get-fn-source-safe f)]
    (parse-fn-source-info fn-source)))

(defn char-to-subscript [char]
  {:pre [(string? char)
         (= (count char) 1)]}
  (let [char-code (.charCodeAt char 0)                                                                                        ;(ocall char "charCodeAt" 0)
        subscript-code (+ 8321 -49 char-code)]                                                                                ; 'SUBSCRIPT ZERO' (U+2080)
    (ocall js/String "fromCharCode" subscript-code)))

(defn decorate [n]
  {:pre [(number? n)]}
  (string/join (map char-to-subscript (str n))))

(defn find-index-of-human-prefix [name]
  (.indexOf name "--"))                                                                                                       ; TODO: better heuristics here

(defn humanize-name [state name]
  (let [index (find-index-of-human-prefix name)]
    (if (> index 0)
      (let [stripped-name (.substring name 0 index)]
        (if-let [decorator (get state stripped-name)]
          (-> state
              (update :result conj (str stripped-name (decorate decorator)))
              (update stripped-name inc))
          (-> state
              (update :result conj stripped-name)
              (assoc stripped-name 1))))
      (update state :result conj name))))

(defn humanize-names [names]
  (with-meta (:result (reduce humanize-name {:result []} names)) (meta names)))

(defn get-fn-fixed-arity [f n]
  (oget f (str "cljs$core$IFn$_invoke$arity$" n)))

(defn get-fn-variadic-arity [f]
  (oget f (str "cljs$core$IFn$_invoke$arity$variadic")))

(defn collect-fn-fixed-arities [f max]
  (loop [arity 0
         collection {}]
    (if (> arity max)
      collection
      (if-let [arity-fn (get-fn-fixed-arity f arity)]
        (recur (inc arity) (assoc collection arity arity-fn))
        (recur (inc arity) collection)))))

(defn collect-fn-variadic-arity [f]
  (if-let [variadic-arity (get-fn-variadic-arity f)]
    {:variadic variadic-arity}))

(defn collect-fn-arities [f]
  (if-let [max-fixed-arity (oget f "cljs$lang$maxFixedArity")]
    (let [fixed-arities (collect-fn-fixed-arities f max-fixed-arity)
          variadic-arity (collect-fn-variadic-arity f)]
      (merge fixed-arities variadic-arity))))

(defn arities-to-args-lists* [arities]
  (let [comparator (fn [x y]
                     (cond
                       (and (keyword? x) (keyword? y)) (cond
                                                         (= :variadic x) 1
                                                         (= :variadic y) -1
                                                         :else (compare (name x) (name y)))
                       (keyword? x) 1
                       (keyword? y) -1
                       :else (compare x y)))
        sorted-keys (sort comparator (keys arities))
        sorted-fns (map #(get arities %) sorted-keys)
        infos (map parse-fn-info sorted-fns)
        args-lists (map #(drop 2 %) infos)]
    (if (= (last sorted-keys) :variadic)
      (concat (butlast args-lists) [(vary-meta (last args-lists) assoc :variadic true)])
      args-lists)))

(defn arities-to-args-lists [arities & [humanize?]]
  (let [args-lists (arities-to-args-lists* arities)]
    (if humanize?
      (map humanize-names args-lists)
      args-lists)))

(defn args-lists-to-strings [args-lists]
  (let [printer (fn [args-list]
                  (if (:variadic (meta args-list))
                    (string/trim (str (string/join " " (butlast args-list)) " & " (last args-list)))
                    (string/join " " args-list)))]
    (map printer args-lists)))