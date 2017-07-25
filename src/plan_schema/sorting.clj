;; Copyright © 2017 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.sorting
  "Sorted map implementations supporting homogeneous and heterogeneous keys"
  (:import [java.lang
            Number]
           [java.util
            Comparator]
           [java.io
            Serializable]
           [clojure.lang
            Numbers]))

;; homogeneous keys

(defn sort-map
  "Returns a homogeneous key map (including any nested maps) in sorted order"
  {:added "0.3.3"}
  ([v]
   (cond
     (map? v)
     (reduce-kv sort-map (sorted-map) v)
     :else v))
  ([m k v]
   (assoc m k
     (cond
       (map? v)
       (reduce-kv sort-map (sorted-map) v)
       :else v))))

;; heterogenous keys (slower than for homogeneneous keys)

;; A=nil B=Number C=Boolean K=Keyword S=String Y=Symbol Z=other
(defn class-name-for-compare [o]
  (if (nil? o)
    "A" ;; "nil"
    (if (number? o)
      (str "B=" (double o))
      (str
        (get {"java.lang.Boolean" "C"
              "clojure.lang.Keyword" "K"
              "java.lang.String" "S"
              "clojure.lang.Symbol" "Y"} (.getName (class o)) "Z")
        "=" o))))

(defn compare-by-class
  "Compare two objects. If they are of the same class use the class
   specific Comparator else compare on the class name"
  [o1 o2]
  (if (nil? o1)
    (if (nil? o2)
      0   ;; nil == nil
      -1) ;; nil < non-nil
    (if (nil? o2)
      1   ;; non-nil > nil
      (if (number? o1)
        (if (number? o2)
          (Numbers/compare o1 o2) ;; numeric comparison
          -1) ;; numbers before others (except nil)
        (if (number? o2)
          1 ;; numbers before others (except nil)
          (let [c1 (class o1)
                c2 (class o2)]
            (if (= c1 c2) ;; same class?
              (.compareTo o1 o2)     ;; non-numeric comparison
              (.compareTo ;; compare on class name
                (class-name-for-compare c1)
                (class-name-for-compare c2)))))))))

(declare default-comparator-by-class)

;; This is a special "interface' of Serializable
;; http://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html
(definterface ReadResolve
  (readResolve []))

;; implements Comparator, Serializable
(deftype comparator-by-class []
  Comparator
  (compare [_ o1 o2]
    (compare-by-class o1 o2))
  ReadResolve ;; Serializable
  (readResolve [_]
    default-comparator-by-class))

(def default-comparator-by-class (->comparator-by-class))

(defn sorted-map-by-class
  "Like sorted-map, but uses compare-by-class such that maps with
   heterogeneously typed keys will always sort into a consistent, stable order"
  [& keyvals]
  (apply sorted-map-by default-comparator-by-class keyvals))

(defn sort-mixed-map
  "Returns a heterogeneous key map (including any nested maps) in sorted order"
  {:added "0.3.6"}
  ([v]
   (cond
     (map? v)
     (reduce-kv sort-mixed-map (sorted-map-by-class) v)
     :else v))
  ([m k v]
   (assoc m k
     (cond
       (map? v)
       (reduce-kv sort-mixed-map (sorted-map-by-class) v)
       :else v))))
