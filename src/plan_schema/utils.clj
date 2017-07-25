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
            [me.raynes.fs :as fs]
            [plan-schema.sorting :refer [sort-map]])
  (:import [java.io ;; for fs-file-name
            File]
           [java.util.regex ;; for match-eval-out-err
            Pattern]))

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

(defn read-json-str [s]
  (reduce (fn [res [k v]]
            (if (map? v)
              (conj res {(keyword k) (as-keywords v)})
              (conj res {(keyword k) v})))
          {}
          (json/read-str s)))

(defn write-json-str [m]
  (with-out-str (json/pprint (sort-map m))))

(defn error?
  "Returns error string from operation (or nil on success)"
  {:added "0.3.1"}
  [output]
  (cond
    (map? output)
    (:error output)
    (vector? output) ;; it's a merge with two plans.. this is fine
    false
    ;(and (string? output) (empty? output))
    ;false
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

;; This is a complement to me.raynes.fs and will return the
;; basename of the file path as a string
(defn fs-basename [path]
  (let [file (if (= (type path) File) path (File. path))]
    (.getName file)))

;; This is a complement to me.raynes.fs and will return the
;; dirname of the file path as a string
(defn fs-dirname [path]
  (let [file (if (= (type path) File) path (File. path))]
    (or (.getParent file) ".")))

;; This helper function fs-get-path will get the path for a file
;; and, optionally, remove a prefix (if present)
(defn fs-get-path [path & [prefix]]
  (let [file (if (= (type path) File) path (File. path))
        path (.getPath file)]
    (if prefix
      (string/replace path prefix "")
      path)))

;; The macro match-eval-out-err is intended for testing and
;; really belongs in a separate library. Putting it in the plan-schema
;; library proper allows this harness to be re-used in other PAMELA
;; related projects without duplication.

(def pattern-opts
  {:canon-eq Pattern/CANON_EQ
   :case-insensitive Pattern/CASE_INSENSITIVE
   :comments Pattern/COMMENTS
   :dotall Pattern/DOTALL
   :literal Pattern/LITERAL
   :multiline Pattern/MULTILINE
   :unicode-case Pattern/UNICODE_CASE
   :unix-lines Pattern/UNIX_LINES
   :invert 1024})

(defn pattern-flag? [flags flag]
  (not (zero? (bit-and flags (get pattern-opts flag 0))  )))

;; expanded version of re-pattern that can take options
;; see also
;; http://docs.oracle.com/javase/1.5.0/docs/api/java/util/regex/Pattern.html
;; :invert means invert the match (i.e. the string should NOT match the re)
(defn re-pattern-opts [s & options]
  (let [flags (reduce (fn [a b] (bit-or a (get pattern-opts b 0))) 0 options)]
    (Pattern/compile s flags)))

(def stdout-ignore nil)

(def stderr-ignore nil)

(def stdout-empty #"^$")

(def stderr-empty #"^$")

(def stderr-no-errors (re-pattern-opts "error" :case-insensitive :invert))

;; out-expect should
;;   nil if we don't care about matching stdout
;;   #"regex" -- a string (converted to regex) or regex that stdout should match
;;     NOTE newlines are stripped from stdout before the match such that
;;     the expression '.*' will match across lines
;; err-expect is analogous to out-expect, for stderr
;; the return value is [rv out-match err-match]
;; where
;;    rv is the value of the evaluated body
;;    out-match is true for a match (or if we don't care),
;;       else it is an error string explaining the match failed
;;    err-match is analogous to out-match, but for stderr

(defmacro match-eval-out-err
  "Evaluates exprs in a context in which *out* and *err* are bound to a fresh
  StringWriter.  Prints *out* and *err* after execution."
  [out-expect err-expect & body]
  `(let [body# (println "  EVAL" (pr-str (first (quote ~body))))
         out# (new java.io.StringWriter)
         err# (new java.io.StringWriter)
         [rv# out-str# err-str#]
         (binding [*out* out#
                   *err* err#]
           (let [exception# (atom nil)
                 raw-rv# (try
                           ~@body
                           (catch Throwable e#
                             (reset! exception# (.getMessage e#))
                             nil))
                 raw-out# (str out#)
                 raw-err# (str err#)
                 except-err# (if @exception#
                               (str "EXCEPTION:\n" @exception#
                                 "\nERROR:\n" raw-err#)
                               raw-err#)]
             [raw-rv# raw-out# except-err#]))
         out-pattern# (cond
                        (nil? ~out-expect)
                        nil
                        (string? ~out-expect)
                        (re-pattern ~out-expect) ;; promote to pattern
                        (instance? java.util.regex.Pattern ~out-expect)
                        ~out-expect
                        :else
                        nil)
         out-flags# (if out-pattern# (.flags out-pattern#) 0)
         out-inverted# (pattern-flag? out-flags# :invert)
         out-rv# (or (not out-pattern#) ;; we don't care about out
                   (if ((if out-inverted# not identity)
                        (re-find out-pattern# out-str#))
                     true ;; we have a match
                     (str (if out-inverted#
                            "stdout DID match inverted "
                            "stdout did NOT match ")
                       "'" ~out-expect "' ===\n"
                       out-str# "\n===")))
         err-pattern# (cond
                        (nil? ~err-expect)
                        nil
                        (string? ~err-expect)
                        (re-pattern ~err-expect) ;; promote to pattern
                        (instance? java.util.regex.Pattern ~err-expect)
                        ~err-expect
                        :else
                        nil)
         err-flags# (if err-pattern# (.flags err-pattern#) 0)
         err-inverted# (pattern-flag? err-flags# :invert)
         err-rv# (or (not err-pattern#) ;; we don't care about err
                   (if ((if err-inverted# not identity)
                        (re-find err-pattern# err-str#))
                     true ;; we have a match
                     (str (if err-inverted#
                            "stderr DID match inverted "
                            "stderr did NOT match ")
                       "'" ~err-expect "' ===\n"
                       err-str# "\n===")))]
     ;; DEBUG
     ;; (println "OUT=>" out-str# "<=OUT")
     ;; (println "ERR=>" err-str# "<=ERR")
     ;; (println "MATCH?" err-pattern# "="(re-find err-pattern# err-str#))
     [rv# out-rv# err-rv#]))
