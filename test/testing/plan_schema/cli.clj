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
            [plan-schema.cli :as cli]
            [plan-schema.utils :refer [fs-get-path
                                       match-eval-out-err stderr-no-errors
                                       stdout-ignore stderr-ignore
                                       stdout-empty stderr-empty]]))

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
          (match-eval-out-err #"^\{:hedge-58" stderr-no-errors
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

    ;; These tests are not applicable because
    ;; -- The merged output is derived from htn and tpn
    ;; --  the output of merge is used by planviz/om-next and
    ;; we have no need to go between json <--> edn for merged content.
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err #"^\[\{:edge/edge-by-plid-id" stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-i" tpn "-f" "edn" "-o" "-" "merge")
    ;;         )))
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err stdout-empty stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-i" tpn "-f" "edn"
    ;;           "-o" (fs-get-path (fs/file target-cli "main.merge.edn") top-path)
    ;;           "merge")
    ;;         )))

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
          (match-eval-out-err #"^\"\{\\\"hedge-58\\\":" stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json" "-o" "-" "htn")
            )))
    (is (= [0 true true]
          (match-eval-out-err stdout-empty stderr-no-errors
            (cli/plan-schema "-i" htn "-f" "json"
              "-o" (fs-get-path (fs/file target-cli "main.htn.json") top-path)
              "htn")
            )))

    ;; NOTE: these tests are currently disabled as write-json-str
    ;; does *not* preserve namespaced keywords
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err #"^\"\{\\\"edge/edge-by-plid-id\\\":"
    ;;         stderr-no-errors
    ;;         (cli/plan-schema "-i" tpn "-f" "json" "-o" "-" "tpn-plan")
    ;;         )))
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err stdout-empty stderr-no-errors
    ;;         (cli/plan-schema "-i" tpn "-f" "json"
    ;;           "-o" (fs-get-path (fs/file target-cli "main2.tpn.json") top-path)
    ;;           "tpn-plan")
    ;;         )))

    ;; (is (= [0 true true]
    ;;       (match-eval-out-err #"^\"\{\\\"edge/edge-by-plid-id\\\":"
    ;;         stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-f" "json" "-o" "-" "htn-plan")
    ;;         )))
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err stdout-empty stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-f" "json"
    ;;           "-o" (fs-get-path (fs/file target-cli "main2.htn.json") top-path)
    ;;           "htn-plan")
    ;;         )))

    ;; These tests are not applicable because
    ;; -- The merged output is derived from htn and tpn
    ;; --  the output of merge is used by planviz/om-next and
    ;; we have no need to go between json <--> edn for merged content.
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err #"^\"\[\{\\\"edge/edge-by-plid-id\\\":"
    ;;         stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-i" tpn "-f" "json" "-o" "-" "merge")
    ;;         )))
    ;; (is (= [0 true true]
    ;;       (match-eval-out-err stdout-empty stderr-no-errors
    ;;         (cli/plan-schema "-i" htn "-i" tpn "-f" "json"
    ;;           "-o" (fs-get-path (fs/file target-cli "main.merge.json") top-path)
    ;;           "merge")
    ;;         )))

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
