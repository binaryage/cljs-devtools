(ns devtools.munging
  (:require [clojure.string :as string]))

(defmacro get-fast-path-protocols []
  (pr-str cljs.core/fast-path-protocols))

(defn get-protocol-info [protocol]
  (let [full-name (str protocol)
        [ns name] (string/split full-name #"/")
        selector (str (string/join "." (map munge (string/split ns #"\."))) "." (munge name))]
    [ns name selector]))

(defmacro get-fast-path-protocols-lookup-table []
  (let [protocols cljs.core/fast-path-protocols
        * (fn [accum [protocol [partition bit]]]
            (update accum partition (fn [partition-map]
                                      (assoc partition-map bit (get-protocol-info protocol)))))]
    (reduce * {} protocols)))

(defmacro get-fast-path-protocol-partitions-count []
  cljs.core/fast-path-protocol-partitions-count)
