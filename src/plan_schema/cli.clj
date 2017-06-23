;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.cli
  "Temporal Planning Network schema command line interface"
  (:require [clojure.string :as string]
            [clojure.java.io :refer :all] ;; for as-file
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs :as fs]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]]
            [plan-schema.core :as pschema]
            [plan-schema.utils :as putils :refer [error? stdout-option?
                                                  log-trace log-debug log-info
                                                  log-warn log-error]])
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
    :parse-fn #(-> % fs/expand-home str)
    :assoc-fn (fn [m k v]
                (let [oldv (get m k [])
                      oldv (if (= oldv ["-"]) [] oldv)]
                  (assoc m k (conj oldv v))))]
   ["-o" "--output OUTPUT" "Output file" ;;  (or - for STDOUT)"
    :default "-"
    :parse-fn #(-> % fs/expand-home str)]
   ["-s" "--strict" "Enforce strict plan schema checking"]
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

(def test-mode false)
(defn set-test-mode! [value]
  (def test-mode value))

(defn repl?
  "Helper function returns true if on the REPL (for development)"
  {:added "0.2.0"}
  []
  (= (:pager env) "cat"))

(defn exit
  "Exit plan-schema with given status code (and optional messages)."
  {:added "0.1.0"}
  [status & msgs]
  (when msgs
    (if (zero? status)
      (println (string/join \newline msgs))
      (log-error \newline (string/join \newline msgs))))
  (flush) ;; ensure all pending output has been flushed
  (if (or (repl?) test-mode)
    (log-warn "exit" status "plan-schema in DEV MODE. Not exiting ->" "repl?" (repl?) "test-mode" test-mode)
    (do (shutdown-agents)
        (System/exit status)))
  status)

(defn do-action [action options]
  (let [out (action options)
        error (error? out)]
    ;; if we've found an error then it's likely it's already been
    ;; reported... output the first line just in case we have
    ;; a new error from error? itself.
    (if error
      (do
        (log-error (first (re-find #"^.*(\n)?" error)))
        (exit 1))
      (do
        (when (stdout-option? (:output options))
          (pprint out))
        (exit 0)))))

(defn plan-schema
  "plan-schema command line processor. (see usage for help)."
  {:added "0.1.0"
   :version "0.3.3"}
  [& args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args cli-options)
        cmd (first arguments)
        action (get actions cmd)
        {:keys [help version verbose file-format input output strict]} options
        cwd (or (:plan-schema-cwd env) (:user-dir env))
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
    (putils/set-strict! strict)
    (when (and verbose? (not exit?))
      (when (> verbose 1)
        (println "repl?:" (repl?))
        (println "test-mode:" test-mode)
        (println "cwd:" cwd)
        (println "version:" (:plan-schema-version env)))
      (println "verbosity level:" verbose)
      (println "file-format:" file-format)
      (println "input:" input)
      (println "output:" output)
      (println "strict:" strict)
      (println "cmd:" cmd (if action "(valid)" "(invalid)")))
    (if-not action
      (if-not exit?
        (exit 1 (str "Unknown action: \"" cmd "\". Must be one of " (keys actions)))
        (usage summary))
      (if (> verbose 1) ;; throw full exception with stack trace when -v -v
        (do-action action options)
        (try
          (do-action action options)
          (catch Throwable e ;; note AssertionError not derived from Exception
            (exit 1 "caught exception: " (.getMessage e))))))))

(defn -main
  "plan-schema main"
  {:added "0.1.0"}
  [& args]
  (apply plan-schema args))
