;; Copyright © 2017 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.plan-schema.sorting
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.pprint :refer [pprint]]
            [plan-schema.sorting :refer :all]))

(defn add-value [m k]
  (assoc m k
    (str (if (nil? k) "nil" k) " is a "
      (class-name-for-compare k))))

(deftest test-sort-map
  (testing "test sort-map"
    (let [values [:b :c :e :a :d]
          plain-map (reduce add-value {} values)
          plain-map (assoc plain-map
                      :b plain-map
                      :d plain-map)
          expected-shallow-map {:a ":a is a K=:a",
                                :b
                                {:b ":b is a K=:b",
                                 :c ":c is a K=:c",
                                 :e ":e is a K=:e",
                                 :a ":a is a K=:a",
                                 :d ":d is a K=:d"},
                                :c ":c is a K=:c",
                                :d
                                {:b ":b is a K=:b",
                                 :c ":c is a K=:c",
                                 :e ":e is a K=:e",
                                 :a ":a is a K=:a",
                                 :d ":d is a K=:d"},
                                :e ":e is a K=:e"}
          ordered-map (into (sorted-map) plain-map) ;; shallow
          expected-deep-map {:a ":a is a K=:a",
                             :b
                             {:a ":a is a K=:a",
                              :b ":b is a K=:b",
                              :c ":c is a K=:c",
                              :d ":d is a K=:d",
                              :e ":e is a K=:e"},
                             :c ":c is a K=:c",
                             :d
                             {:a ":a is a K=:a",
                              :b ":b is a K=:b",
                              :c ":c is a K=:c",
                              :d ":d is a K=:d",
                              :e ":e is a K=:e"},
                             :e ":e is a K=:e"}
          sorted-map (sort-map plain-map)] ;; deep
      (is (= expected-shallow-map ordered-map))
      (is (= expected-deep-map sorted-map))
      )))

(deftest test-sort-mixed-map
  (testing "test sort-mixed-map"
    (let [values [nil true false 22/7 0 3.14  :b :c :a 'f 'e 'd "h" "i" "g"]
          plain-map (reduce add-value {} values)
          expected-map {nil "nil is a A",
                        0 "0 is a B=0.0",
                        3.14 "3.14 is a B=3.14",
                        22/7 "22/7 is a B=3.142857142857143",
                        :a ":a is a K=:a",
                        :b ":b is a K=:b",
                        :c ":c is a K=:c",
                        'd "d is a Y=d",
                        'e "e is a Y=e",
                        'f "f is a Y=f",
                        false "false is a C=false",
                        true "true is a C=true",
                        "g" "g is a S=g",
                        "h" "h is a S=h",
                        "i" "i is a S=i"}
          expected-map-keys '(nil 0 3.14 22/7 :a :b :c d e f false true "g" "h" "i")
          ordered-map (into (sorted-map-by-class) plain-map) ;; shallow
          sorted-map (sort-mixed-map plain-map)] ;; deep
      (is (= expected-map ordered-map))
      (is (= expected-map-keys (keys ordered-map)))
      (is (= expected-map sorted-map))
      (is (= expected-map-keys (keys sorted-map)))
      )))
