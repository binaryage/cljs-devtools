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

(defn get-fn-source-safely [f]
  (try
    (ocall f "toString")
    (catch :default _
      "")))

(defn trivial-fn-source? [fn-source]
  {:pre [(string? fn-source)]}
  (or (some? (re-matches #"function\s*\(\s*\)\s*\{\s*\}\s*" fn-source))                                                       ; that space between ) and { is important to distingush built-in fns and user-specified fns
      (some? (re-matches #"function.*\(\)\s*\{\s*\[native code\]\s*\}\s*" fn-source))))

(defn cljs-fn? [f]
  (if (fn? f)
    (let [name (oget f name)]
      (if-not (empty? name)
        (cljs-fn-name? name)
        (let [fn-source (get-fn-source-safely f)]
          (let [[name] (parse-fn-source fn-source)]
            (if-not (empty? name)
              (cljs-fn-name? name)
              (not (trivial-fn-source? fn-source)))))))))                                                                     ; we assume non-trivial anonymous functions to come from cljs

(defn dollar-preserving-demunge [munged-name]
  (-> munged-name
      (string/replace "$" dollar-replacement)
      (demunge)                                                                                                               ; note: demunge is too aggressive in replacing dollars
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
      (concat [ns name] demunged-args))
    ["" ""]))

(defn parse-fn-info [f]
  (let [fn-source (get-fn-source-safely f)]
    (parse-fn-source-info fn-source)))

(declare collect-fn-arities)

(defn parse-fn-info-deep [f]
  (let [fn-info (parse-fn-info f)
        arities (collect-fn-arities f)]
    (if (some? arities)
      (if (> (count arities) 1)
        (concat (take 2 fn-info) ::multi-arity)
        (concat (take 2 fn-info) (drop 2 (parse-fn-info-deep (second (first arities))))))
      fn-info)))

(defn char-to-subscript [char]
  {:pre [(string? char)
         (= (count char) 1)]}
  (let [char-code (ocall (js/String. char) "charCodeAt" 0)                                                                    ; this is an ugly trick to overcome a bug, char string may not be a string "object"
        subscript-code (+ 8322 -49 char-code)]                                                                                ; 'SUBSCRIPT ZERO' (U+2080), start with subscript '2'
    (ocall js/String "fromCharCode" subscript-code)))

(defn decorate-with-subscript [subscript]
  {:pre [(number? subscript)]}
  (string/join (map char-to-subscript (str subscript))))

(defn find-index-of-human-prefix [name]
  (let [sep-start (.indexOf name "--")
        num-prefix (count (second (re-find #"(.*?)\d{2,}" name)))
        finds (filter pos? [sep-start num-prefix])]
    (if-not (empty? finds)
      (apply min finds))))

(defn humanize-name [state name]
  (let [index (find-index-of-human-prefix name)]
    (if (> index 0)
      (let [stripped-name (.substring name 0 index)]
        (if-let [subscript (get state stripped-name)]
          (-> state
              (update ::result conj (str stripped-name (decorate-with-subscript subscript)))
              (update stripped-name inc))
          (-> state
              (update ::result conj stripped-name)
              (assoc stripped-name 1))))
      (update state ::result conj name))))

(defn humanize-names [names]
  (with-meta (::result (reduce humanize-name {::result []} names)) (meta names)))

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
    {::variadic variadic-arity}))

(defn collect-fn-arities [f]
  (let [max-fixed-arity (oget f "cljs$lang$maxFixedArity")
        fixed-arities (collect-fn-fixed-arities f (or max-fixed-arity 64))                                                    ; we cannot rely on cljs$lang$maxFixedArity when people implement IFn protocol
        variadic-arity (collect-fn-variadic-arity f)
        result (merge fixed-arities variadic-arity)]
    (if-not (empty? result)
      result)))

(defn arities-key-comparator [x y]
  (let [kx? (keyword? x)
        ky? (keyword? y)]
    (cond
      (and kx? ky?) (cond
                      (= ::variadic x) 1
                      (= ::variadic y) -1
                      :else (compare (name x) (name y)))
      kx? 1
      ky? -1
      :else (compare x y))))

(defn arities-to-args-lists* [arities]
  (let [sorted-keys (sort arities-key-comparator (keys arities))
        sorted-fns (map #(get arities %) sorted-keys)
        infos (map parse-fn-info-deep sorted-fns)
        args-lists (map #(drop 2 %) infos)]
    (if (= (last sorted-keys) ::variadic)
      (concat (butlast args-lists) [(vary-meta (last args-lists) assoc ::variadic true)])
      args-lists)))

(defn arities-to-args-lists [arities & [humanize?]]
  (let [args-lists (arities-to-args-lists* arities)]
    (if humanize?
      (map humanize-names args-lists)
      args-lists)))

(defn args-lists-to-strings [args-lists spacer-symbol multi-arity-symbol rest-symbol]
  (let [mapper #(case %
                 ::multi-arity multi-arity-symbol
                 %)
        printer (fn [args-list]
                  (let [args-strings (map mapper args-list)]
                    (if (::variadic (meta args-strings))
                      (string/trim (str (string/join spacer-symbol (butlast args-strings)) rest-symbol (last args-strings)))
                      (string/join spacer-symbol args-strings))))]
    (map printer args-lists)))