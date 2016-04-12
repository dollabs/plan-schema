;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.plan-schema.core
  (:require [clojure.string :as string]
            #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is]])
            [plan-schema.core :as pschema]))

(deftest test-json
  (testing "test json"
    (let [json-str "{\"a\":123}"]
      (is (= json-str
            (-> json-str
              (pschema/read-json-str)
              (pschema/write-json-str)
              (string/trim-newline)))))))
