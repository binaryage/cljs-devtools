(ns devtools.munging
  (:require [clojure.string :as string]))

(defn steal-fast-path-protocols []
  ; fast-path-protocols made private in https://github.com/clojure/clojurescript/commit/627f7fd513d928531db48392d8f52142fea5eb38
  @#'cljs.core/fast-path-protocols)

(defn steal-fast-path-protocol-partitions-count []
  ; fast-path-protocol-partitions-count made private in https://github.com/clojure/clojurescript/commit/627f7fd513d928531db48392d8f52142fea5eb38
  @#'cljs.core/fast-path-protocol-partitions-count)

(defmacro get-fast-path-protocols []
  (pr-str (steal-fast-path-protocols)))

(defn get-protocol-info [protocol]
  (let [full-name (str protocol)
        [ns name] (string/split full-name #"/")
        selector (str (string/join "." (map munge (string/split ns #"\."))) "." (munge name))]
    [ns name selector]))

(defmacro get-fast-path-protocols-lookup-table []
  (let [protocols (steal-fast-path-protocols)
        * (fn [accum [protocol [partition bit]]]
            (update accum partition (fn [partition-map]
                                      (assoc partition-map bit (get-protocol-info protocol)))))]
    (reduce * {} protocols)))

(defmacro get-fast-path-protocol-partitions-count []
  (steal-fast-path-protocol-partitions-count))
