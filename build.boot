;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(def project 'dollabs/plan-schema)
(def version "0.3.8")
(def description "Temporal Planning Network schema utilities")
(def project-url "https://github.com/dollabs/plan-schema")
(def main 'plan-schema.cli)

(set-env!
  :resource-paths #{"src"}
  :source-paths   #{"test"}
  :dependencies   '[[org.clojure/clojure         "1.8.0"   :scope "provided"]
                    [environ                     "1.1.0"]
                    [org.clojure/tools.cli       "0.3.5"]
                    [prismatic/schema            "1.1.5"]
                    [org.clojure/data.json       "0.2.6"]
                    [avenir                      "0.2.2"]
                    [me.raynes/fs                "1.4.6"]
                    ;; testing/development
                    [adzerk/boot-test            "1.2.0"          :scope "test"]
                    [adzerk/bootlaces            "0.1.13"         :scope "test"]
                    ;; api docs
                    [boot-codox                  "0.10.3"         :scope "test"]
                    ])

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.bootlaces :refer :all]
  '[codox.boot :refer [codox]])

(bootlaces! version)

(task-options!
  pom {:project     project
       :version     version
       :description description
       :url         project-url
       :scm         {:url project-url}
       :license     {"Apache-2.0" "http://opensource.org/licenses/Apache-2.0"}}
  aot {:namespace   #{main}}
  jar {:main        main}
  test {:namespaces #{'testing.plan-schema.cli
                      'testing.plan-schema.core
                      'testing.plan-schema.sorting}}
  codox {:language :clojure
         :source-paths ["src"]
         :name (name project)
         :version version
         :output-path  "doc/api"
         :source-uri (str project-url "/blob/master/{filepath}#L{line}")})

(deftask testing
  "merge source paths in for testing"
  []
  (merge-env! :source-paths #{"test"})
  identity)

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp
      (sift :include #{#"~$"} :invert true) ;; don't include emacs backups
      (aot)
      (pom)
      (jar)
      (target :dir dir))))

(deftask local
  "Build jar and install to local repo."
  []
  (comp
    (sift :include #{#"~$"} :invert true) ;; don't include emacs backups
    (aot)
    (pom)
    (jar)
    (install)))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require [main :as 'app])
  (let [argv (if (pos? (count args))
               (clojure.string/split (first args) #" ")
               '())]
    (apply (resolve 'app/-main) argv)
    identity))

;; (deftask cider-boot
;;   "Cider boot params task"
;;   []
;;   ;; (cider))
;;   (comp
;;     (cider)
;;     (repl :server true)
;;     (wait)))
