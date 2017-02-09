;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.plan-schema.core
  (:require [clojure.string :as string]
            [clojure.data :as data]
            #?(:clj
               [clojure.test :refer [deftest testing is]]
               :cljs
               [cljs.test :as test :refer-macros [deftest testing is]])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint float?]])
            [plan-schema.core :as pschema]
            #?(:clj [plan-schema.cli :as cli])
            ))

(deftest working-directory
  (testing "Current Working Directory"
    (println "Current working directory:"
      #?(:clj (System/getProperty "user.dir")
         :cljs "."))))

; Pair of files in edn and json format that should match when read by plan schema
(def test-files [["test/data/main.htn.edn" "test/data/main.htn.json" :htn]
                 ["test/data/main.tpn.edn" "test/data/main.tpn.json" :tpn]])

(defn execute-action [cwd in-file out-file file-format]
  (let [options {:cwd cwd :input in-file :output out-file :file-format file-format}
        action #?(:clj (get cli/actions (name file-format))
                  :cljs identity)]
    ;(println "options" options)
    ;(println "action" action)
    ;(println "file-format" file-format)
    (action options)))

#?(:clj

   (deftest test-schema-reading
     (testing "JSON Coercion test")
     (doseq [files test-files]
       (let [[edn json file-format] files
             parsed-edn (str edn ".clj")
             parsed-json (str json ".clj")
             cwd (System/getProperty "user.dir")
             out-edn (execute-action cwd edn "-" file-format)
             out-json (execute-action cwd json "-" file-format)
             [in-edn in-json in-both] (data/diff out-edn out-json)
             ]

         (println "file set" edn json file-format)

         (when-not (= out-edn out-json)
           (println "Files differ" parsed-edn parsed-json)
           (spit parsed-edn (with-out-str (pprint out-edn)))
           (spit parsed-json (with-out-str (pprint out-json)))
           (is false (prn-str "Files differ" parsed-edn parsed-json))))))

   )

(deftest test-json
  (testing "test json"
    (let [json-str "{\"a\":123}"]
      (is (= json-str
             (-> json-str
                 (pschema/read-json-str)
                 (pschema/write-json-str)
                 (string/trim-newline)))))))
