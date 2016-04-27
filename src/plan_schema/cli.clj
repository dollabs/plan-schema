;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.cli
  "Temporal Planning Network schema command line interface"
  (:require [clojure.string :as string]
            [clojure.java.io :refer :all] ;; for as-file
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]]
            [plan-schema.core :as pschema])
  (:import [java.security
            PrivilegedActionException]) ;; for debugging
  (:gen-class))

(def #^{:added "0.1.0"}
  actions
  "Valid plan-schema command line actions"
  {"tpn" (var pschema/parse-tpn)
   "htn" (var pschema/parse-htn)
   "tpn-plan" (var pschema/tpn-plan)
   "htn-plan" (var pschema/htn-plan)
   "merge" (var pschema/merge-networks)})

(def #^{:added "0.1.0"}
  output-formats
  "Valid plan-schema output file formats"
  #{"edn" "json"})

(def #^{:added "0.1.0"}
  cli-options
  "Command line options"
  [["-h" "--help" "Print usage"]
   ["-V" "--version" "Print plan-schema version"]
   ["-v" "--verbose" "Increase verbosity"
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-f" "--file-format FORMAT" "Output file format"
    :default "edn"
    :validate [#(contains? output-formats %)
               (str "FORMAT not supported, must be one of "
                 (vec output-formats))]]
   ["-i" "--input INPUT" "Input file(s)" ;;  (or - for STDIN)"
    :default ["-"]
    ;; NOTE: might have HTN=TPN
    ;; :validate [#(or (= "-" %) (.exists (as-file %)))
    ;;            "INPUT file does not exist"]
    :assoc-fn (fn [m k v]
                (let [oldv (get m k [])
                      oldv (if (= oldv ["-"]) [] oldv)]
                  (assoc m k (conj oldv v))))]
   ["-o" "--output OUTPUT" "Output file" ;;  (or - for STDOUT)"
    :default "-"]
   ])

(defn usage
  "Print plan-schema command line help.

  **plan-schema** utilities

  Usage: **plan-schema** *[options]* action

  **Options:**

  - -h, --help              **Print usage**
  - -V, --version           **Print PLAN-SCHEMA version**
  - -v, --verbose           **Increase verbosity**
  - -f, --file-format *FORMAT*   **Output file format (one of edn, json)**
  - -i, --input *INPUT*     **Input file(s) (or - for STDIN)**
  - -o, --output *OUTPUT*   **Output file (or - for STDOUT)**

  **Actions:**

  - **hpn**	Parse HTN
  - **tpn**	Parse TPN
  "
  {:added "0.1.0"}
  [options-summary]
  (->> (for [a (sort (keys actions))]
         (str "  " a "\t" (:doc (meta (get actions a)))))
    (concat [""
             "plan-schema"
             ""
             "Usage: plan-schema [options] action"
             ""
             "Options:"
             options-summary
             ""
             "Actions:"])
    (string/join \newline)))

(defn repl?
  "Helper function returns true if on the REPL (for development)"
  {:added "0.2.0"}
  []
  (= (:pager env) "cat"))

(defn exit
  "Exit plan-schema with given status code (and optional messages)."
  {:added "0.1.0"}
  [status & msgs]
  (if msgs (println (string/join \newline msgs)))
  (when (repl?)
    (throw (Exception. (str "DEV MODE exit(" status ")"))))
  (shutdown-agents)
  (System/exit status)
  true)

(defn plan-schema
  "plan-schema command line processor. (see usage for help)."
  {:added "0.1.0"
   :version "0.1.0"}
  [& args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args cli-options)
        cmd (first arguments)
        action (get actions cmd)
        {:keys [help version verbose file-format input output]} options
        cwd (or (:plan-schema-cwd env) (:user-dir env))
        output (if-not (pschema/stdout-option? output)
                 (if (.startsWith output "/")
                   output ;; absolute
                   (str cwd "/" output)))
        options (assoc options :output output :cwd cwd
                  :file-format (keyword file-format))
        verbose? (pos? (or verbose 0))
        exit?
        (cond
          help
          (exit 0 (usage summary))
          errors
          (exit 1 (string/join \newline errors) (usage summary))
          version
          (exit 0 (:version (meta #'plan-schema)))
          (not= (count arguments) 1)
          (exit 1 "Specify exactly one action" (usage summary)))]
    (when (and verbose? (not exit?))
      (when (> verbose 1)
        (println "repl?:" (repl?))
        (println "cwd:" cwd)
        (println "version:" (:plan-schema-version env)))
      (println "verbosity level:" verbose)
      (println "file-format:" file-format)
      (println "input:" input)
      (println "output:" output)
      (println "cmd:" cmd (if action "(valid)" "(invalid)")))
    (if-not action
      (if-not exit?
        (exit 1 (str "Unknown action: \"" cmd "\". Must be one of " (keys actions)))
        (usage summary))
      (try
        (action options)
        (catch Throwable e ;; note AssertionError not derived from Exception
          ;; (catch PrivilegedActionException e
          ;; NOTE: this alternate exception is to help generate a stack trace
          ;; for debugging purposes
          ;; FIXME: use proper logging
          (binding [*out* *err*]
            (println "ERROR caught exception:" (.getMessage e)))
          (exit 1))))
    (exit 0)))

(defn -main
  "plan-schema main"
  {:added "0.1.0"}
  [& args]
  (apply plan-schema args))
