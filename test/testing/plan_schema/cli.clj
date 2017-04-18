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
            [plan-schema.cli :as cli]))

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

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" tpn "-f" "edn"
                 "-o" "-"
                 "tpn"))))
    (is (= 0 (cli/plan-schema "-i" tpn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main.tpn.edn") top-path)
               "tpn")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-f" "edn"
                 "-o" "-"
                 "htn"))))
    (is (= 0 (cli/plan-schema "-i" htn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main.htn.edn") top-path)
               "htn")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" tpn "-f" "edn"
                 "-o" "-"
                 "tpn-plan"))))
    (is (= 0 (cli/plan-schema "-i" tpn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main2.tpn.edn") top-path)
               "tpn-plan")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-f" "edn"
                 "-o" "-"
                 "htn-plan"))))
    (is (= 0 (cli/plan-schema "-i" htn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main2.htn.edn") top-path)
               "htn-plan")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-i" tpn "-f" "edn"
                 "-o" "-"
                 "merge"))))
    (is (= 0 (cli/plan-schema "-i" htn "-i" tpn "-f" "edn"
               "-o" (fs-get-path (fs/file target-cli "main.merge.edn") top-path)
               "merge")))

    ;; fail EDN --------------------------------------------------------

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
                 "-o" "-"
                 "tpn"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus.tpn.edn") top-path)
               "tpn")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
                 "-o" "-"
                 "htn"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus.htn.edn") top-path)
               "htn")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
                 "-o" "-"
                 "tpn-plan"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus2.tpn.edn") top-path)
               "tpn-plan")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
                 "-o" "-"
                 "htn-plan"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus2.htn.edn") top-path)
               "htn-plan")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
                 "-f" "json"
                 "-o" "-"
                 "merge"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.fred.edn" "-i" "bogus.tpn.edn"
               "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "fail.merge.edn") top-path)
               "merge")))

    ;; pass JSON  --------------------------------------------------------

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" tpn "-f" "json"
                 "-o" "-"
                 "tpn"))))
    (is (= 0 (cli/plan-schema "-i" tpn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main.tpn.json") top-path)
               "tpn")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-f" "json"
                 "-o" "-"
                 "htn"))))
    (is (= 0 (cli/plan-schema "-i" htn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main.htn.json") top-path)
               "htn")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" tpn "-f" "json"
                 "-o" "-"
                 "tpn-plan"))))
    (is (= 0 (cli/plan-schema "-i" tpn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main2.tpn.json") top-path)
               "tpn-plan")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-f" "json"
                 "-o" "-"
                 "htn-plan"))))
    (is (= 0 (cli/plan-schema "-i" htn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main2.htn.json") top-path)
               "htn-plan")))

    (is (= 0 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-i" tpn "-f" "json"
                 "-o" "-"
                 "merge"))))
    (is (= 0 (cli/plan-schema "-i" htn "-i" tpn "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "main.merge.json") top-path)
               "merge")))

    ;; fail JSON  --------------------------------------------------------


    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
                 "-o" "-"
                 "tpn"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus.tpn.json") top-path)
               "tpn")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
                 "-o" "-"
                 "htn"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus.htn.json") top-path)
               "htn")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
                 "-o" "-"
                 "tpn-plan"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.tpn.edn" "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus2.tpn.json") top-path)
               "tpn-plan")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
                 "-o" "-"
                 "htn-plan"))))
    (is (= 1 (cli/plan-schema "-i" "bogus.htn.edn""-f" "json"
               "-o" (fs-get-path (fs/file target-cli "bogus2.htn.json") top-path)
               "htn-plan")))

    (is (= 1 (binding [*out* (new java.io.StringWriter)] ;; discard *out*
               (cli/plan-schema "-i" htn "-i" "bogus.foo.json"
                 "-f" "json"
                 "-o" "-"
                 "merge"))))
    (is (= 1 (cli/plan-schema "-i" htn "-i" "bogus.foo.json"
               "-f" "json"
               "-o" (fs-get-path (fs/file target-cli "fail.merge.json") top-path)
               "merge")))

    ))
