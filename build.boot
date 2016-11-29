;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(def project 'dollabs/plan-schema)
(def version "0.2.14")
(def description "Temporal Planning Network schema utilities")
(def project-url "https://github.com/dollabs/plan-schema")
(def main 'plan-schema.cli)

(set-env!
  :resource-paths #{"src"}
  :source-paths   #{"test"}
  :dependencies   '[[org.clojure/clojure         "1.8.0"   :scope "provided"]
                    [org.clojure/clojurescript   "1.9.293" :scope "provided"]
                    [environ                     "1.1.0"]
                    [org.clojure/tools.cli       "0.3.5"]
                    [prismatic/schema            "1.1.3"]
                    [org.clojure/data.json       "0.2.6"]
                    [avenir                      "0.2.1"]
                    [me.raynes/fs                "1.4.6"]
                    ;; cljs-dev
                    [com.cemerick/piggieback     "0.2.1"          :scope "test"]
                    [weasel                      "0.7.0"          :scope "test"]
                    [org.clojure/tools.nrepl     "0.2.12"         :scope "test"]
                    [adzerk/boot-reload          "0.4.13"         :scope "test"]
                    [pandeiro/boot-http          "0.7.6"          :scope "test"]
                    [adzerk/boot-cljs            "1.7.228-2"      :scope "test"]
                    [adzerk/boot-cljs-repl       "0.3.3"          :scope "test"]
                    ;; testing/development
                    [adzerk/boot-test            "1.1.2"          :scope "test"]
                    [crisptrutski/boot-cljs-test "0.3.0-SNAPSHOT" :scope "test"]
                    [adzerk/bootlaces            "0.1.13"         :scope "test"]
                    ;; api docs
                    [boot-codox                  "0.10.2"         :scope "test"]
                    ])

(require
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl repl-env]]
  '[pandeiro.boot-http :refer [serve]]
  '[adzerk.boot-reload    :refer [reload]]
  '[adzerk.boot-test :refer [test]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]]
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
  cljs {:source-map true}
  test-cljs {:js-env :phantom
             :namespaces #{"testing.plan-schema.core"}}
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

(deftask tests
  "Test CLJS and leave artifacts in target for debugging"
  [e js-env VAL kw "Set the :js-env for test-cljs (:phantom)."]
  (comp
    (sift :add-resource #{"html"})
    (testing)
    (test-cljs :js-env (or js-env :phantom))
    (target :dir #{"target"})))

(deftask testc
  "Run both CLJ tests and CLJS tests"
  [e js-env VAL kw "Set the :js-env for test-cljs (:phantom)."]
  (comp
    (test)
    (tests :js-env (or js-env :phantom))))

(deftask build-cljs
  "Compile ClojureScript"
  []
  (comp
    (sift :include #{#"~$"} :invert true) ;; don't include emacs backups
    (cljs)
    (target :dir #{"target"})))

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
