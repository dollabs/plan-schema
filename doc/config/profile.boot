(set-env!
  :dependencies '[[org.clojure/clojure   "1.8.0"     :scope "provided"]
                  [adzerk/boot-reload    "0.4.13"    :scope "test"]
                  [pandeiro/boot-http    "0.7.6"     :scope "test"]
                  [adzerk/boot-cljs      "1.7.228-2" :scope "test"]
                  [adzerk/boot-cljs-repl "0.3.3"     :scope "test"]
                  ])

(require
  '[boot.repl      :as repl]
  '[clojure.java.io      :as io]
  ;; extra
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl]]
  '[pandeiro.boot-http :refer [serve]]
  '[adzerk.boot-reload    :refer [reload]]
  )

(deftask cider "CIDER profile"
  []
  (swap! @(resolve 'boot.repl/*default-dependencies*)
    concat '[[org.clojure/tools.nrepl "0.2.12"]
             [cider/cider-nrepl "0.15.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
             [refactor-nrepl "2.3.0-SNAPSHOT"]
             ])
  (swap! @(resolve 'boot.repl/*default-middleware*)
    concat '[cider.nrepl/cider-middleware
             refactor-nrepl.middleware/wrap-refactor])
  identity)

(defn- generate-lein-project-file!
  [& {:keys [keep-project] :or {:keep-project true}}]
  ;; (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
  (let [pfile (io/file "project.clj")
        ;; Only works when pom options are set using task-options!
        {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
        prop #(when-let [x (get-env %2)] [%1 x])
        head (list* 'defproject (or project 'boot-project)
               (or version "0.0.0-SNAPSHOT")
               (concat
                 (prop :url :url)
                 (prop :license :license)
                 (prop :description :description)
                 [:dependencies (get-env :dependencies)
                  :source-paths (vec (concat (get-env :source-paths)
                                       (get-env :resource-paths)))]))
        proj (pp-str head)]
    (if-not keep-project (.deleteOnExit pfile))
    (spit pfile proj)))

(deftask lein-generate
  "Generate a leiningen `project.clj` file.
   This task generates a leiningen `project.clj` file based on the boot
   environment configuration, including project name and version (generated
   if not present), dependencies, and source paths. Additional keys may be added
   to the generated `project.clj` file by specifying a `:lein` key in the boot
   environment whose value is a map of keys-value pairs to add to `project.clj`."
  []
  (generate-lein-project-file! :keep-project true))

;; CIDER helper tasks

(deftask cljs-dev
  "ClojureScript Browser REPL for CIDER"
  []
  (comp
    (cider)
    (serve :dir "target/public")
    (watch)
    (reload)
    (cljs-repl) ;; before cljs
    (cljs)
    (target :dir #{"target"})))

(deftask clj-dev
  "Clojure REPL for CIDER"
  []
  (comp
    (cider)
    (repl :server true)
    (wait)))

;; returns true to start a CIDER CLJS session (else CLJ)
(defn cider-cljs? []
  false
  ;; true
  )

(deftask cider-boot
  "Cider boot params task"
  []
  (if (cider-cljs?)
    (cljs-dev)
    (clj-dev)))
