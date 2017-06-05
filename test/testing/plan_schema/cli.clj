;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.plan-schema.cli
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [me.raynes.fs :as fs]
            [plan-schema.core :as pschema]
            [plan-schema.cli :as cli])
  (:import [java.util.regex
            Pattern]))

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

(defn fs-get-path [file & [prefix]]
  (let [path (.getPath file)]
    (if prefix
      (string/replace path prefix "")
      path)))

;; (deftest testing-repl-mode-on
;;   (is (= "cat" (:pager env))))

(deftest testing-plan-schema-cli
  (testing "testing-plan-schema-cli")
  (let [htn "test/data/main.htn.edn"
        tpn "test/data/main.tpn.edn"
        top (fs/file (:user-dir env))
        top-path (str (fs-get-path top) "/")
        target-cli (fs/file top "target" "cli")]

    (cli/set-test-mode! true) ;; no need to set repl-mode

    (if-not (fs/exists? target-cli)
      (fs/mkdirs target-cli))

    ;; pass EDN ----------------------------

    (is (= [0 true true]
          (match-eval-out-err #"^\{:act-11" stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "edn" "-o" "-" "tpn")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main.tpn.edn") top-path)
               "tpn")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\{:hedge-64" stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "edn" "-o" "-" "htn")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "main.htn.edn") top-path)
              "htn")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\{:edge/edge-by-plid-id" stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "edn" "-o" "-" "tpn-plan")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "main2.tpn.edn") top-path)
              "tpn-plan")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\{:edge/edge-by-plid-id" stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "edn" "-o" "-" "htn-plan")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "main2.htn.edn") top-path)
              "htn-plan")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\[\{:edge/edge-by-plid-id" stderr-no-errors
            (cli/plan-schema "-i" htn "-i" tpn "-f" "edn" "-o" "-" "merge")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-i" tpn "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "main.merge.edn") top-path)
              "merge")
            )))

    ;; fail EDN --------------------------------------------------------

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "edn" "-o" "-" "tpn")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "bogus.tpn.edn") top-path)
              "tpn")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn" "-f" "edn" "-o" "-" "htn")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "bogus.htn.edn") top-path)
              "htn")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "edn" "-o" "-"
              "tpn-plan")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "bogus2.tpn.edn") top-path)
              "tpn-plan")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "edn" "-o" "-" "htn-plan")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "bogus2.htn.edn") top-path)
              "htn-plan")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "ERROR HTN file not one of:"
            (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
              "-f" "edn" "-o" "-" "merge")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "ERROR HTN file not one of:"
            (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
              "-f" "edn"
              "-o" (fs-get-path (fs/file target-cli "incorrect.merge.edn") top-path)
              "merge")
            )))

    ;; pass JSON  --------------------------------------------------------

    (is (= [0 true true]
          (match-eval-out-err #"^\"\{\\\"act-11\\\":" stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "json" "-o" "-" "tpn")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main.tpn.json") top-path)
               "tpn")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\"\{\\\"hedge-64\\\":" stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json" "-o" "-" "htn")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "main.htn.json") top-path)
              "htn")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\"\{\\\"edge/edge-by-plid-id\\\":"
            stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "json" "-o" "-" "tpn-plan")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" tpn "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "main2.tpn.json") top-path)
              "tpn-plan")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\"\{\\\"edge/edge-by-plid-id\\\":"
            stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json" "-o" "-" "htn-plan")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "main2.htn.json") top-path)
              "htn-plan")
            )))

    (is (= [0 true true]
          (match-eval-out-err #"^\"\[\{\\\"edge/edge-by-plid-id\\\":"
            stderr-no-errors
            (cli/plan-schema "-i" htn "-i" tpn "-f" "json" "-o" "-" "merge")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-i" tpn "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "main.merge.json") top-path)
              "merge")
            )))

    ;; fail JSON  --------------------------------------------------------

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json" "-o" "-" "tpn")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "bogus.tpn.edn") top-path)
              "tpn")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn" "-f" "json" "-o" "-" "htn")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
              "-o" (fs-get-path (fs/file target-cli "bogus.htn.edn") top-path)
              "htn")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json" "-o" "-"
              "tpn-plan")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.tpn.edn"
            (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "bogus2.tpn.edn") top-path)
              "tpn-plan")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "json" "-o" "-" "htn-plan")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "input does not exist: bogus.htn.edn"
            (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
              "-o" (fs-get-path (fs/file target-cli "bogus2.htn.edn") top-path)
              "htn-plan")
            )))

    (is (= [1 true true]
          (match-eval-out-err stdout-empty "ERROR HTN file not one of:"
            (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
              "-f" "json" "-o" "-" "merge")
            )))
    (is (= [1 true true]
          (match-eval-out-err stdout-empty "ERROR HTN file not one of:"
            (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
              "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "incorrect.merge.edn") top-path)
              "merge")
            )))
    ))
