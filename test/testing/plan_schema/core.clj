;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.plan-schema.core
  (:require [clojure.string :as string]
            [clojure.data :as data]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [clojure.pprint :refer [pprint]]
            [plan-schema.core :as pschema]
            [plan-schema.cli :as cli]
            [plan-schema.utils :as utils]))

(deftest working-directory
  (testing "Current Working Directory"
    (println "Current working directory:"
      (System/getProperty "user.dir"))))

; Pair of files in edn and json format that should match when read by plan schema
(def test-files [["test/data/main.htn.edn" "test/data/main.htn.json" :htn]
                 ["test/data/main.tpn.edn" "test/data/main.tpn.json" :tpn]])

(defn execute-action [cwd in-file out-file file-format]
  (let [options {:cwd cwd :input in-file :output out-file :file-format file-format}
        action (get cli/actions (name file-format))]
    ;(println "options" options)
    ;(println "action" action)
    ;(println "file-format" file-format)
    (action options)))

(deftest test-schema-reading
  (testing "JSON Coercion test")
  (doseq [files test-files]
    (let [[edn json file-format] files
          parsed-edn (str "target/" edn ".clj")
          parsed-json (str "target/" json ".clj")
          cwd (System/getProperty "user.dir")
          out-edn (execute-action cwd edn "-" file-format)
          out-json (execute-action cwd json "-" file-format)
          [in-edn in-json in-both] (data/diff out-edn out-json)
          ]

      (println "file set" edn json file-format)

      (when-not (= out-edn out-json)
        (println "Files differ" parsed-edn parsed-json)
        (io/make-parents parsed-edn)
        (spit parsed-edn (with-out-str (pprint out-edn)))
        (spit parsed-json (with-out-str (pprint out-json)))
        (is false (prn-str "Files differ" parsed-edn parsed-json))))))

(defn do-coerce [infile file-format]
  ; file-format is keyword, :htn or :tpn
  (execute-action (System/getProperty "user.dir") infile "-" file-format)
  nil)

(deftest test-json
  (testing "test json"
    (let [json-str "{\"a\":123}"]
      (is (= json-str
             (-> json-str
                 (utils/read-json-str)
                 (utils/write-json-str)
                 (string/trim-newline)))))
    (= {} (utils/read-json-str "{}"))
    (= {:a 123 :m {"string" "value"}} (utils/read-json-str "{\"a\": 123, \"m\": {\"string\": \"value\"}}"))
    ))
