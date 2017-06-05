;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.utils
  "Temporal Planning Network schema utilities"
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [avenir.utils :as au
             :refer [keywordize assoc-if concatv]]
            [me.raynes.fs :as fs]))


(defn sort-map
  "Ensures that it an all values which are maps are in sorted order"
  {:added "0.3.3"}
  ([v]
   (cond
     (map? v)
     (into (sorted-map) (reduce-kv sort-map {} v))
     :else v))
  ([m k v]
   (assoc m k
     (cond
       (map? v)
       (into (sorted-map) (reduce-kv sort-map {} v))
       :else v))))

(defn synopsis [s]
  (let [max-len 256
        s (if (string? s) s (str s))]
    (if (> (count s) max-len)
      (str (subs s 0 max-len) " ...")
      s)))

(def configuration (atom {:strict false
                          :loggers {}}))

(defn strict? []
  (:strict @configuration))

(defn set-strict! [strict]
  (swap! configuration assoc :strict strict))

;; log-fn should take variable arity
(defn set-logger! [level log-fn]
  (swap! configuration assoc-in [:loggers level] log-fn))

(defn logger [level & msgs]
  (let [log-fn (get-in @configuration [:loggers level])]
    (if (fn? log-fn)
      (apply log-fn msgs)
      (binding [*out* *err*]
        (println (string/upper-case (name level))
          (string/join " " msgs))))))

(defn log-trace [& msgs]
  (apply logger :trace msgs))

(defn log-debug [& msgs]
  (apply logger :debug msgs))

(defn log-info [& msgs]
  (apply logger :info msgs))

(defn log-warn [& msgs]
  (apply logger :warn msgs))

(defn log-error [& msgs]
  (apply logger :error msgs))

(defn as-keywords [m]
  (reduce (fn [res [k v]]
            (conj res {(keyword k) v}))
    {} m))

;; see the docs
;; http://clojure.github.io/data.json/#clojure.data.json/read-str
;; and the code
;; https://github.com/clojure/data.json/blob/master/src/main/clojure/clojure/data/json.clj#L246
(defn read-json-str
  ;; ([v]
  ;;  (read-json-str (json/read-str v :key-fn keyword))))
  ([v]
   (cond
     (map? v)
     (reduce-kv read-json-str {} v)
     (vector? v)
     (mapv read-json-str v)
     (string? v)
     (try ;; could be a JSON string?
       (read-json-str (json/read-str v :key-fn keyword))
       (catch Throwable e ;; nope, not a JSON string
         v))
     :else
     v))
  ([m k v]
   (assoc m k ;; this was already done with :key-fn (keyword k)
     (cond
       (map? v)
       (reduce-kv read-json-str {} v)
       (vector? v)
       (mapv read-json-str v)
       :else
       v))))

;; see
;; http://clojure.github.io/data.json/#clojure.data.json/write
;; https://clojuredocs.org/clojure.core/name
(defn write-json-key-fn [k]
  (cond
    (keyword? k)
    (string/replace k #"^:" "") ;; coerce to string, preserve namespace
    (symbol? k)
    (name k)
    (string? k)
    k
    :else
    (str k)))

(defn write-json-str [m]
  (with-out-str
    (json/pprint (sort-map m)
      :key-fn write-json-key-fn
      :escape-slash false)))

(defn error?
  "Returns error string from operation (or nil on success)"
  {:added "0.3.1"}
  [output]
  (cond
    (map? output)
    (:error output)
    (vector? output) ;; it's a merge with two plans.. this is fine
    false
    (and (string? output) (empty? output))
    false
    (string? output) ;; assume it's JSON as a string
    (:error (read-json-str output))
    (nil? output)
    "Output is nil."
    :else
    (str "Unknown output format type: " (type output))))

(defn stdout-option?
  "Returns true if the output file name is STDOUT"
  {:added "0.1.0"}
  [output]
  (or (empty? output) (= output "-")))
