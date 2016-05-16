;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.core
  "Temporal Planning Network schema utilities"
  (:require [clojure.string :as string]
            [clojure.set :as set]
            #?(:clj [clojure.data.json :as json])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint float?]])
            #?(:clj [avenir.utils :as au
                     :refer [keywordize assoc-if concatv]]
               :cljs [avenir.utils :as au
                      :refer [keywordize assoc-if concatv format remove-fn]])
            [schema.core :as s #?@(:cljs [:include-macros true])]
            [schema.coerce :as coerce]
            [schema.utils :as su]
            [schema.spec.core :as spec]
            #?(:clj [me.raynes.fs :as fs])
            ))

#?(:cljs (enable-console-print!))

(defn stdout-option?
  "Returns true if the output file name is STDOUT"
  {:added "0.1.0"}
  [output]
  (or (empty? output) (= output "-")))

(defn read-json-str
  ([s]
   (read-json-str s true))
  ([s keywordize?]
   ((if keywordize? keywordize identity)
    #?(:clj
       (json/read-str s)
       :cljs
       (js->clj (.parse js/JSON s))))))

(defn write-json-str [m]
  #?(:clj
     (with-out-str (json/pprint m))
     :cljs
     (.stringify js/JSON (clj->js m))))

;; TPN-------------------------------------------------------------------

(defn network-id? [x]
  (or (keyword? x) (string? x)))

(s/defschema network-id
  "network-id"
  s/Keyword)

(def check-network-id (s/checker network-id))

(defn network? [x]
  (and (map? x)
    (#{:network
       "network"
       "NETWORK"} (get x :tpn-type))))

(s/defschema eq-network?
  "eq-network?"
  (s/conditional
    keyword? (s/eq :network)
    #(and (string? %)
       (= "network" (string/lower-case %))) s/Keyword
    'eq-network?))

(s/defschema network
  "An network"
  {:tpn-type eq-network?
   :uid s/Keyword
   :begin-node s/Keyword
   (s/optional-key :end-node) s/Keyword})

(def check-network (s/checker network))

(s/defschema non-primitive
  "A non-primitive is false or a network"
  (s/conditional
    false? (s/eq false)
    keyword? s/Keyword
    string? s/Keyword
    'non-primitive?))

(def check-non-primitive (s/checker non-primitive))

(s/defschema upper-bound
  "upper-bound"
  (s/conditional
    #(= :infinity %) (s/eq :infinity)
    number? s/Num
    'upper-bound?))

(def check-upper-bound (s/checker upper-bound))

(s/defschema bounds
  "Temporal bounds"
  [(s/one s/Num "lower-bound")
   (s/one upper-bound "upper-bound")])

(def check-bounds (s/checker bounds))

(s/defschema between
  "between constraint [from to]"
  [(s/one s/Keyword "between-from-label")
   (s/one s/Keyword "between-to-label")])

(def check-between (s/checker between))

(defn temporal-constraint? [x]
  (and (map? x)
    (#{:temporal-constraint
       "temporal-constraint"
       "TEMPORAL-CONSTRAINT"} (get x :tpn-type))))

(s/defschema eq-temporal-constraint?
  "eq-temporal-constraint?"
  (s/conditional
    keyword? (s/eq :temporal-constraint)
    #(and (string? %)
       (= "temporal-constraint" (string/lower-case %))) s/Keyword
    'eq-temporal-constraint?))

(s/defschema temporal-constraint
  "A temporal constraint"
  {:tpn-type eq-temporal-constraint?
   :uid s/Keyword
   :value bounds
   :end-node s/Keyword
   (s/optional-key :between) between
   (s/optional-key :between-ends) between
   (s/optional-key :between-starts) between})

(def check-temporal-constraint (s/checker temporal-constraint))

(defn cost<=-constraint? [x]
  (and (map? x)
    (#{:cost<=-constraint
       "cost<=-constraint"
       "COST<=-CONSTRAINT"} (get x :tpn-type))))

(s/defschema eq-cost<=-constraint?
  "eq-cost<=-constraint?"
  (s/conditional
    keyword? (s/eq :cost<=-constraint)
    #(and (string? %)
       (= "cost<=-constraint" (string/lower-case %))) s/Keyword
    'eq-cost<=-constraint?))

(s/defschema cost<=-constraint
  "A cost<= constraint"
  {:tpn-type eq-cost<=-constraint?
   :uid s/Keyword
   :value s/Num
   :end-node s/Keyword
   (s/optional-key :between) between
   (s/optional-key :between-ends) between
   (s/optional-key :between-starts) between})

(def check-cost<=-constraint (s/checker cost<=-constraint))

(defn reward>=-constraint? [x]
  (and (map? x)
    (#{:reward>=-constraint
       "reward>=-constraint"
       "REWARD>=-CONSTRAINT"} (get x :tpn-type))))

(s/defschema eq-reward>=-constraint?
  "eq-reward>=-constraint?"
  (s/conditional
    keyword? (s/eq :reward>=-constraint)
    #(and (string? %)
       (= "reward>=-constraint" (string/lower-case %))) s/Keyword
    'eq-reward>=-constraint?))

(s/defschema reward>=-constraint
  "A reward>= constraint"
  {:tpn-type eq-reward>=-constraint?
   :uid s/Keyword
   :value s/Num
   :end-node s/Keyword
   (s/optional-key :between) between
   (s/optional-key :between-ends) between
   (s/optional-key :between-starts) between})

(def check-reward>=-constraint (s/checker reward>=-constraint))

(defn activity? [x]
  (and (map? x)
    (#{:activity
       "activity"
       "ACTIVITY"} (get x :tpn-type))))

(s/defschema eq-activity?
  "eq-activity?"
  (s/conditional
    keyword? (s/eq :activity)
    #(and (string? %)
       (= "activity" (string/lower-case %))) s/Keyword
    'eq-activity?))

(s/defschema activity
  "An activity"
  {:tpn-type eq-activity?
   :uid s/Keyword
   :constraints #{s/Keyword}
   :end-node s/Keyword
   (s/optional-key :name) s/Str
   (s/optional-key :label) s/Keyword ;; label for between
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :cost) s/Num
   (s/optional-key :reward) s/Num
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :network-flows) #{s/Keyword}
   (s/optional-key :plant) s/Str
   (s/optional-key :plantid) s/Str
   (s/optional-key :command) s/Str
   (s/optional-key :non-primitive) non-primitive
   (s/optional-key :order) s/Num ;; order of activity
   })

(def check-activity (s/checker activity))

(s/defschema flow-characteristics
  "Flow Characteristics"
  {s/Keyword s/Str})

(defn network-flow? [x]
  (and (map? x)
    (#{:network-flow
       "network-flow"
       "NETWORK-FLOW"} (get x :tpn-type))))

(s/defschema eq-network-flow?
  "eq-network-flow?"
  (s/conditional
    keyword? (s/eq :network-flow)
    #(and (string? %)
       (= "network-flow" (string/lower-case %))) s/Keyword
    'eq-network-flow?))

(s/defschema network-flow
  "An network-flow"
  {:tpn-type eq-network-flow?
   :uid s/Keyword
   :start-enclave s/Keyword
   :end-enclaves #{s/Keyword}
   :qos-attributes s/Any ;; FIXME
   :flow-characteristics flow-characteristics})

(def check-network-flow (s/checker network-flow))

(defn null-activity? [x]
  (and (map? x)
    (#{:null-activity
       "null-activity"
       "NULL-ACTIVITY"} (get x :tpn-type))))

(s/defschema eq-null-activity?
  "eq-null-activity?"
  (s/conditional
    keyword? (s/eq :null-activity)
    #(and (string? %)
       (= "null-activity" (string/lower-case %))) s/Keyword
    'eq-null-activity?))

(s/defschema null-activity
  "An null-activity"
  {:tpn-type eq-null-activity?
   :uid s/Keyword
   :end-node s/Keyword
   (s/optional-key :label) s/Keyword  ;; label for between
   (s/optional-key :probability) s/Num
   (s/optional-key :cost) s/Num
   (s/optional-key :reward) s/Num
   (s/optional-key :guard) s/Str
   (s/optional-key :order) s/Num ;; order of activity
   })

(def check-null-activity (s/checker null-activity))

(defn state? [x]
  (and (map? x)
    (#{:state
       "state"
       "STATE"} (get x :tpn-type))))

(s/defschema eq-state?
  "eq-state?"
  (s/conditional
    keyword? (s/eq :state)
    #(and (string? %)
       (= "state" (string/lower-case %))) s/Keyword
    'eq-state?))

(s/defschema state
  "An state"
  {:tpn-type eq-state?
   :uid s/Keyword
   :constraints #{s/Keyword}
   :activities #{s/Keyword} ;; probably wants to be a vector, not a set
   :incidence-set #{s/Keyword}
   (s/optional-key :label) s/Keyword ;; label for between
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :htn-node) s/Keyword ;; added by the merge operation
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   })

(def check-state (s/checker state))

(defn c-begin? [x]
  (and (map? x)
    (#{:c-begin
       "c-begin"
       "C-BEGIN"} (get x :tpn-type))))

(s/defschema eq-c-begin?
  "eq-c-begin?"
  (s/conditional
    keyword? (s/eq :c-begin)
    #(and (string? %)
       (= "c-begin" (string/lower-case %))) s/Keyword
    'eq-c-begin?))

(s/defschema c-begin
  "An c-begin"
  {:tpn-type eq-c-begin?
   :uid s/Keyword
   :constraints #{s/Keyword}
   :activities #{s/Keyword} ;; probably wants to be a vector, not a set
   :incidence-set #{s/Keyword}
   :end-node s/Keyword
   (s/optional-key :label) s/Keyword ;; label for between
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :probability) s/Num
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   })


(def check-c-begin (s/checker c-begin))

(defn c-end? [x]
  (and (map? x)
    (#{:c-end
       "c-end"
       "C-END"} (get x :tpn-type))))

(s/defschema eq-c-end?
  "eq-c-end?"
  (s/conditional
    keyword? (s/eq :c-end)
    #(and (string? %)
       (= "c-end" (string/lower-case %))) s/Keyword
    'eq-c-end?))

(s/defschema c-end
  "An c-end"
  {:tpn-type eq-c-end?
   :uid s/Keyword
   :activities #{s/Keyword} ;; probably wants to be a vector, not a set
   :incidence-set #{s/Keyword}
   (s/optional-key :constraints) #{s/Keyword}
   (s/optional-key :probability) s/Num
   (s/optional-key :begin) s/Keyword ;; new, points to c-begin
   })

(def check-c-end (s/checker c-end))

(defn p-begin? [x]
  (and (map? x)
    (#{:p-begin
       "p-begin"
       "P-BEGIN"} (get x :tpn-type))))

(s/defschema eq-p-begin?
  "eq-p-begin?"
  (s/conditional
    keyword? (s/eq :p-begin)
    #(and (string? %)
       (= "p-begin" (string/lower-case %))) s/Keyword
    'eq-p-begin?))

(s/defschema p-begin
  "An p-begin"
  {:tpn-type eq-p-begin?
   :uid s/Keyword
   :constraints #{s/Keyword}
   :activities #{s/Keyword} ;; probably wants to be a vector, not a set
   :incidence-set #{s/Keyword}
   :end-node s/Keyword
   (s/optional-key :label) s/Keyword ;; label for between
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   })

(def check-p-begin (s/checker p-begin))

(defn p-end? [x]
  (and (map? x)
    (#{:p-end
       "p-end"
       "P-END"} (get x :tpn-type))))

(s/defschema eq-p-end?
  "eq-p-end?"
  (s/conditional
    keyword? (s/eq :p-end)
    #(and (string? %)
       (= "p-end" (string/lower-case %))) s/Keyword
    'eq-p-end?))

(s/defschema p-end
  "An p-end"
  {:tpn-type eq-p-end?
   :uid s/Keyword
   :activities #{s/Keyword} ;; probably wants to be a vector, not a set
   :incidence-set #{s/Keyword}
   (s/optional-key :constraints) #{s/Keyword}
   (s/optional-key :begin) s/Keyword ;; new, points to p-begin
   })

(def check-p-end (s/checker p-end))

(s/defschema tpn-object
  "One of the valid TPN object types"
  (s/conditional
    network-id? network-id
    network? network
    temporal-constraint? temporal-constraint
    cost<=-constraint? cost<=-constraint
    reward>=-constraint? reward>=-constraint
    activity? activity
    null-activity? null-activity
    network-flow? network-flow
    state? state
    c-begin? c-begin
    c-end? c-end
    p-begin? p-begin
    p-end? p-end
    'tpn-object?))

(def check-tpn-object (s/checker tpn-object))

(s/defschema tpn
  "A TPN"
  {s/Keyword tpn-object})

(def check-tpn (s/checker tpn))

;; HTN -------------------------------------------------------------------

(defn htn-network-id? [x]
  (or (keyword? x) (string? x)))

(s/defschema htn-network-id
  "network"
  s/Keyword)

(def check-htn-network-id (s/checker htn-network-id))

(defn edge? [x]
  (and (map? x)
    (#{:edge "edge" "EDGE"} (get x :type))))

(s/defschema eq-edge?
  "eq-edge?"
  (s/conditional
    keyword? (s/eq :edge)
    #(and (string? %) (= "edge" (string/lower-case %)))
    s/Keyword
    'eq-edge?))

(s/defschema edge
  "An edge"
  {:type eq-edge?
   :uid s/Keyword
   :end-node s/Keyword
   (s/optional-key :edge-type) s/Keyword
   (s/optional-key :label) s/Str
   (s/optional-key :order) s/Num}) ;; order of hedge

(def check-edge (s/checker edge))

(defn htn-network? [x]
  (and (map? x)
    (#{:htn-network
       "htn-network"
       "HTN-NETWORK"} (get x :type))))

(s/defschema eq-htn-network?
  "eq-htn-network?"
  (s/conditional
    keyword? (s/eq :htn-network)
    #(and (string? %) (= "htn-network" (string/lower-case %)))
    s/Keyword
    'eq-htn-network?))

(s/defschema htn-network
  "An htn-network"
  {:type eq-htn-network?
   :uid s/Keyword
   :label s/Str
   :rootnodes #{s/Keyword} ;; probably wants to be a vector, not a set
   (s/optional-key :parentid) s/Keyword})
;; NOTE the parentid points to the parent htn-expanded-method

(def check-htn-network (s/checker htn-network))

(defn htn-primitive-task? [x]
  (and (map? x)
    (#{:htn-primitive-task
       "htn-primitive-task"
       "HTN-PRIMITIVE-TASK"} (get x :type))))

(s/defschema eq-htn-primitive-task?
  "eq-htn-primitive-task?"
  (s/conditional
    keyword? (s/eq :htn-primitive-task)
    #(and (string? %) (= "htn-primitive-task" (string/lower-case %)))
    s/Keyword
    'eq-htn-primitive-task?))

(s/defschema htn-primitive-task
  "An htn-primitive-task"
  {:type eq-htn-primitive-task?
   :uid s/Keyword
   :label s/Str
   :incidence-set #{s/Keyword}
   (s/optional-key :edges) #{s/Keyword}
   (s/optional-key :parent) s/Keyword ;; new
   ;; NOTE the parent points to the parent htn-network
   (s/optional-key :tpn-node) s/Keyword ;; new
   (s/optional-key :tpn-edge) s/Keyword ;; new
   ;; tpn-node points to state, c-begin, or p-begin
   })

(def check-htn-primitive-task (s/checker htn-primitive-task))

(defn htn-expanded-method? [x]
  (and (map? x)
    (#{:htn-expanded-method
       "htn-expanded-method"
       "HTN-EXPANDED-METHOD"} (get x :type))))

(s/defschema eq-htn-expanded-method?
  "eq-htn-expanded-method?"
  (s/conditional
    keyword? (s/eq :htn-expanded-method)
    #(and (string? %) (= "htn-expanded-method" (string/lower-case %)))
    s/Keyword
    'eq-htn-expanded-method?))

(s/defschema htn-expanded-method
  "An htn-expanded-method"
  {:type eq-htn-expanded-method?
   :uid s/Keyword
   :label s/Str
   :incidence-set #{s/Keyword}
   :network s/Keyword
   (s/optional-key :edges) [s/Keyword]
   ;; WAS (s/optional-key :tpn-node) s/Keyword ;; new
   (s/optional-key :tpn-selection) s/Any ;; new
   })

(def check-htn-expanded-method (s/checker htn-expanded-method))

(defn htn-expanded-nonprimitive-task? [x]
  (and (map? x)
    (#{:htn-expanded-nonprimitive-task
       "htn-expanded-nonprimitive-task"
       "HTN-EXPANDED-NONPRIMITIVE-TASK"} (get x :type))))

(s/defschema eq-htn-expanded-nonprimitive-task?
  "eq-htn-expanded-nonprimitive-task?"
  (s/conditional
    keyword? (s/eq :htn-expanded-nonprimitive-task)
    #(and (string? %) (= "htn-expanded-nonprimitive-task" (string/lower-case %)))
    s/Keyword
    'eq-htn-expanded-nonprimitive-task?))

(s/defschema htn-expanded-nonprimitive-task
  "An htn-expanded-nonprimitive-task"
  {:type eq-htn-expanded-nonprimitive-task?
   :uid s/Keyword
   :label s/Str
   :incidence-set #{s/Keyword}
   (s/optional-key :edges) #{s/Keyword}
   (s/optional-key :parent) s/Keyword ;; new
   ;; NOTE the parent points to the parent htn-network
   (s/optional-key :tpn-node) s/Keyword ;; new
   (s/optional-key :tpn-edge) s/Keyword ;; new
   ;; tpn-node points to state, c-begin, or p-begin
   })

(def check-htn-expanded-nonprimitive-task (s/checker htn-expanded-nonprimitive-task))

(s/defschema htn-object
  "One of the valid HTN object types"
  (s/conditional
    htn-network-id? htn-network-id
    htn-network? htn-network
    edge? edge
    htn-primitive-task? htn-primitive-task
    htn-expanded-method? htn-expanded-method
    htn-expanded-nonprimitive-task? htn-expanded-nonprimitive-task
    'htn-object?))

(def check-htn-object (s/checker htn-object))

(s/defschema htn
  "A HTN"
  {s/Keyword htn-object})

(def check-htn (s/checker htn))

;; -------------------------------------------------------------------

(defn walker [schema]
  (spec/run-checker
    (fn [s params]
      (let [walk (spec/checker (s/spec s) params)]
        (fn [x]
          (let [result (walk x)]
            (println (format "%s | checking %s against %s\n"
                       (if (su/error? result) "FAIL" "PASS")
                       x (s/explain s)))
            result))))
    true
    schema))

(defn coercion [schema]
  (spec/run-checker
    (fn [s params]
      (let [walk (spec/checker (s/spec s) params)]
        (fn [x]
          (cond
            (and (string? x)
              (or (= s s/Keyword) (= s upper-bound)))
            (walk (keyword (string/lower-case x)))
            (and (= s #{s/Keyword}) (vector? x))
            (walk (set x))
            :else
            (walk x)))))
    true
    schema))

(def coerce-tpn (coercion tpn))

(def coerce-htn (coercion htn))

(defn kind-filename? [kind filename]
  (not (nil? (string/index-of (string/lower-case filename) kind))))

(defn tpn-filename? [filename]
  (kind-filename? "tpn" filename))

(defn htn-filename? [filename]
  (kind-filename? "htn" filename))

(defn json-filename? [filename]
  (kind-filename? "json" filename))

(defn validate-input [input cwd]
  #?(:clj (if (fs/exists? input)
            input
            (let [cwd-input (str cwd "/" input)]
              (if (fs/exists? cwd-input)
                cwd-input
                {:error (str "input does not exist: " input)})))
     :cljs {:error (str "CLJS input parsing not implemented: " input)}))

;; returns a network map -or- {:error "error message"}
(defn parse-network
  "Parse TPN"
  {:added "0.1.0"}
  [network-type options]
  (let [{:keys [verbose file-format input output cwd]} options
        verbose? (and (not (nil? verbose)) (pos? verbose))
        input (validate-input (if (vector? input) (first input) input) cwd)
        data #?(:clj (if (:error input) input (slurp input))
                :cljs {:error "not implemented yet"})
        data (if (:error data)
               data
               (if (json-filename? input)
                 (read-json-str data)
                 #?(:clj (read-string data)
                    :cljs "not implemented yet")))
        result (if (:error data)
                 data
                 (if (= network-type :htn)
                   (coerce-htn data)
                   (coerce-tpn data)))
        out (if (:error result)
              result
              (if (su/error? result)
                {:error (with-out-str (println (:error result)))}
                result))
        output (if (and (not (stdout-option? output))
                     (not (string/starts-with? output "/")))
                 (str cwd "/" output)
                 output)]
    (if (stdout-option? output)
      ;; NOTE: this isn't really STDOUT, but simply returns the raw value
      (if (= file-format :json)
        (write-json-str out)
        out)
      #?(:clj (spit output
                (if (= file-format :json)
                  (write-json-str out)
                  (with-out-str (pprint out))))
         :cljs (println "not implemented yet")))))

;; returns a map with :tpn on success or :error on failure
(defn parse-tpn
  "Parse TPN"
  {:added "0.1.0"}
  [options]
  (parse-network :tpn options))

(defn parse-htn
  "Parse HTN"
  {:added "0.1.0"}
  [options]
  (parse-network :htn options))

;; -------------------------------------------------------------

(defn name->id [name]
  (keyword (string/replace (string/lower-case name) #"\s+" "_")))

(defn composite-key [k1 k2]
  (keyword (subs (str k1 k2) 1)))

(defn composite-key-fn [k1 k2]
  (fn [props]
    (keyword (subs (str (get props k1) (get props k2)) 1))))

(def node-key-fn (composite-key-fn :plan/plid :node/id))

(def edge-key-fn (composite-key-fn :plan/plid :edge/id))

;; HTN ---------------------

(defn get-node [plan node-id]
  (get-in @plan [:node/node-by-plid-id node-id]))

(defn update-node [plan node]
  (let [plid-id (node-key-fn node)
        ref [:node/node-by-plid-id plid-id]]
    (swap! plan update-in ref merge node)))

(defn get-edge [plan edge-id]
  (get-in @plan [:edge/edge-by-plid-id edge-id]))

(defn update-edge [plan edge]
  (let [plid-id (edge-key-fn edge)
        ref [:edge/edge-by-plid-id plid-id]]
    (swap! plan update-in ref merge edge)))

(declare add-htn-node)

(defn add-htn-edge [plans plan-id network-plid-id edge-id from-plid-id net]
  (let [plid-id (composite-key plan-id edge-id)
        htn-edge (get net edge-id)
        {:keys [end-node label]} htn-edge
        type :sequence-edge
        to-plid-id (composite-key plan-id end-node)
        edge (assoc-if {:plan/plid plan-id
                        :edge/id edge-id
                        :edge/type type
                        :edge/from from-plid-id
                        :edge/to to-plid-id}
               :edge/label label)]
    (swap! plans update-in [:edge/edge-by-plid-id]
      assoc plid-id edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (add-htn-node plans plan-id network-plid-id end-node net)
    nil))

;; nil on success
(defn add-htn-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        htn-node (get net node-id)
        {:keys [type label edges]} htn-node
        node (assoc-if {:plan/plid plan-id
                        :node/id node-id
                        :node/type type
                        :node/parent network-plid-id}
               :node/label label)]
    (swap! plans update-in [:node/node-by-plid-id]
      assoc plid-id node)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/nodes]
      conj plid-id)
    (when-not (empty? edges)
      (doseq [edge edges]
        (add-htn-edge plans plan-id network-plid-id edge plid-id net)))
    nil))

;; nil on success
(defn add-htn-network [plans plan-id network-id net]
  (let [htn-network (get net network-id)
        {:keys [type label rootnodes parentid]} htn-network
        plid-id (composite-key plan-id network-id)
        plid-rootnodes (if rootnodes
                         (set (doall
                                (map (partial composite-key plan-id)
                                  rootnodes))))
        network (assoc-if {:plan/plid plan-id
                           :network/id network-id
                           :network/type type
                           :network/nodes []
                           :network/edges []}
                  :network/label label
                  :network/rootnodes plid-rootnodes
                  :network/parent (composite-key plan-id parentid))]
    (swap! plans update-in [:network/network-by-plid-id]
      assoc plid-id network)
    (swap! plans update-in [:plan/by-plid plan-id :plan/networks]
      conj plid-id)
    (when-not (empty? rootnodes)
      (doseq [rootnode rootnodes]
        (add-htn-node plans plan-id plid-id rootnode net)))
    nil))

(declare add-hem-node)

(defn add-hem-edge [plans plan-id network-plid-id edge-id from-plid-id
                    default-order net]
  (let [plid-id (composite-key plan-id edge-id)
        hem-edge (get net edge-id)
        {:keys [end-node edge-type label order]} hem-edge
        type (if (= edge-type :choice) :choice-edge :parallel-edge)
        to-plid-id (composite-key plan-id end-node)
        edge (assoc-if {:plan/plid plan-id
                        :edge/id edge-id
                        :edge/type type
                        :edge/from from-plid-id
                        :edge/to to-plid-id
                        :edge/order (or order default-order)}
               :edge/label label)]
    (swap! plans update-in [:edge/edge-by-plid-id]
      assoc plid-id edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (add-hem-node plans plan-id network-plid-id end-node net)
    nil))

;; nil on success
(defn add-hem-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        hem-node (get net node-id)
        {:keys [type label network edges]} hem-node
        ;; HERE we assume at some point in the future edges
        ;; will become a vector (because order is important)
        edges (vec edges)
        plid-network (if network (composite-key plan-id network))
        node (assoc-if {:plan/plid plan-id
                        :node/id node-id
                        :node/type type
                        :node/label label}
               :node/htn-network plid-network)]
    (swap! plans update-in [:node/node-by-plid-id]
      assoc plid-id node)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/nodes]
      conj plid-id)
    (when network
      (add-htn-network plans plan-id network net))
    (when-not (empty? edges)
      (doall
        (for [order (range (count edges))
              :let [edge (get edges order)]]
          (do
            (add-hem-edge plans plan-id network-plid-id
              edge plid-id order net)))))
    nil))

(defn unique-id [net prefix]
  (let [id (keyword (gensym prefix))]
    (if (get net id)
      (recur net prefix)
      id)))

(defn add-hem-network [plans plan-id network-id net]
  (when (= network-id :top-hem-network-id)
    (let [hem-network-id (unique-id net "net-") ;; for the hem network
          hem-plid-id (composite-key plan-id hem-network-id)
          begin-id (unique-id net "hid-") ;; for the top hem
          begin-plid-id (composite-key plan-id begin-id)
          htn-network-id (:network net)
          htn-network (get net htn-network-id)
          {:keys [label rootnodes]} htn-network
          htn-plid-id (composite-key plan-id htn-network-id)
          plid-rootnodes (if rootnodes
                           (set (doall
                                  (map (partial composite-key plan-id)
                                    rootnodes))))
          htn-network (assoc-if {:plan/plid plan-id
                                 :network/id htn-network-id
                                 :network/type :htn-network
                                 :network/parent hem-plid-id
                                 :network/nodes []
                                 :network/edges []}
                        :network/label label
                        :network/rootnodes plid-rootnodes)
          begin {:plan/plid plan-id
                 :node/id begin-id
                 :node/type :htn-expanded-method
                 :node/label (or label "HTN")
                 :node/htn-network htn-network-id}
          hem-network {:plan/plid plan-id
                       :network/id hem-network-id
                       :network/type :hem-network
                       :network/begin (composite-key plan-id begin-id)
                       :network/nodes []
                       :network/edges []}]
      (swap! plans update-in [:network/network-by-plid-id]
        assoc hem-plid-id hem-network htn-plid-id htn-network)
      (swap! plans update-in [:plan/by-plid plan-id]
        (fn [p]
          (assoc p
            :plan/networks (conj (:plan/networks p) hem-plid-id htn-plid-id)
            :plan/begin hem-plid-id)))
      ;; add begin hem node
      (swap! plans update-in [:node/node-by-plid-id]
        assoc begin-plid-id begin)
      (swap! plans update-in
        [:network/network-by-plid-id hem-plid-id :network/nodes]
        conj begin-plid-id)
      (loop [edges [] root (first rootnodes) more (rest rootnodes)]
        (if-not root
          ;; add hem edges from begin
          (when-not (empty? edges)
            (doall
              (for [order (range (count edges))
                    :let [edge (get edges order)]]
                (do
                  (add-hem-edge plans plan-id hem-plid-id
                    edge begin-plid-id order net)))))
          ;; add this htn-node
          (let [htn-node (get net root)
                ;; HERE we assume at some point in the future edges
                ;; will become a vector (because order is important)
                edges (vec (:edges htn-node))
                ;; edges (set/union edges (:edges htn-node))
                n-plid-id (composite-key plan-id root)
                node (assoc-if {:plan/plid plan-id
                                :node/id root
                                :node/type (:type htn-node)
                                :node/parent htn-plid-id}
                       :node/label (:label htn-node))]
            (swap! plans update-in [:node/node-by-plid-id]
              assoc n-plid-id node)
            (swap! plans update-in
              [:network/network-by-plid-id htn-plid-id :network/nodes]
              conj n-plid-id)
            (recur edges (first more) (rest more)))))
      nil)))

;; TPN ---------------------

(declare add-tpn-node)

;; nil on success
(defn add-tpn-edge [plans plan-id network-plid-id edge-id from-plid-id to-id
                    net & [default-order]]
  (let [plid-id (composite-key plan-id edge-id)
        net-edge (get net edge-id)
        ;; :temporal-constraint :activity :null-activity
        {:keys [tpn-type end-node constraints
                value ;; value is bounds in :temporal-constraint
                between between-ends between-starts ;; :temporal-constraint
                name label sequence-label sequence-end ;; :activity
                plant plantid command non-primitive ;; :activity
                cost reward ;; :activity :null-activity
                probability guard ;; :null-activity
                network-flows htn-node order]} net-edge
        to-id (or end-node to-id)
        to-plid-id (composite-key plan-id to-id)
        edge (assoc-if {:plan/plid plan-id
                        :edge/id edge-id
                        :edge/type tpn-type
                        :edge/from from-plid-id
                        :edge/to to-plid-id}
               :edge/order (or order default-order)
               :edge/value value ;; bounds for temporal constraint
               :edge/between between
               :edge/between-ends between-ends
               :edge/between-starts between-starts
               :edge/name name
               :edge/label label
               :edge/sequence-label sequence-label
               :edge/sequence-end sequence-end
               :edge/plant plant
               :edge/plantid plantid
               :edge/command command
               :edge/cost cost
               :edge/reward reward
               :edge/probability probability
               :edge/guard guard
               :edge/network-flows network-flows
               :edge/non-primitive non-primitive
               :edge/htn-node htn-node)]
    (swap! plans update-in [:edge/edge-by-plid-id]
      assoc plid-id edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (when-not (empty? constraints)
      (doseq [constraint constraints]
        (add-tpn-edge plans plan-id network-plid-id constraint
          from-plid-id to-id net)))
    (add-tpn-node plans plan-id network-plid-id to-id net)
    ;; FIXME handle network-flows non-primitive
    nil))

;; nil on success
(defn add-tpn-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        node (get-in @plans [:node/node-by-plid-id plid-id])]
    (when-not node
      ;; :state :c-begin :p-begin :c-end :p-end
      (let [net-node (get net node-id)
            {:keys [tpn-type activities ;; mandatory
                    constraints end-node
                    label sequence-label sequence-end
                    probability htn-node]} net-node
            ;; HERE we assume at some point in the future activities
            ;; will become a vector (because order is important)
            activities (vec activities)
            end-node (if end-node (composite-key plan-id end-node))
            node (assoc-if {:plan/plid plan-id
                            :node/id node-id
                            :node/type tpn-type}
                   :node/end end-node
                   :node/label label
                   :node/sequence-label sequence-label
                   :node/sequence-end sequence-end
                   :node/probability probability
                   :node/htn-node htn-node)]
        (swap! plans update-in [:node/node-by-plid-id]
          assoc plid-id node)
        (swap! plans update-in
          [:network/network-by-plid-id network-plid-id :network/nodes]
          conj plid-id)
        (when-not (empty? activities)
          (doall
            (for [order (range (count activities))
                  :let [activity (get activities order)]]
              (add-tpn-edge plans plan-id network-plid-id activity
                plid-id end-node net order))))
        (when-not (empty? constraints)
          (doseq [constraint constraints]
            (add-tpn-edge plans plan-id network-plid-id constraint
              plid-id end-node net)))
        nil))))

(declare find-end)

;; nil on success
(defn add-tpn-network [plans plan-id network-id net]
  (let [net-network (get net network-id)
        {:keys [begin-node end-node]} net-network
        begin-id (composite-key plan-id begin-node)
        end-node (or end-node
                   (:end-node (get net begin-node))
                   ::walk)
        end-id (composite-key plan-id end-node)
        network-plid-id (composite-key plan-id network-id)
        network {:plan/plid plan-id
                 :network/id network-id
                 :network/type :tpn-network
                 :network/begin begin-id
                 :network/end end-id
                 :network/nodes []
                 :network/edges []}]
    (swap! plans update-in [:network/network-by-plid-id]
      assoc network-plid-id network)
    (swap! plans update-in [:plan/by-plid plan-id :plan/networks]
      conj network-plid-id)
    (add-tpn-node plans plan-id network-plid-id begin-node net)
    (when (= end-node ::walk) ;; must walk the graph to find the end node
      (let [end-id (find-end plans plan-id begin-id)]
        (swap! plans assoc-in [:network/network-by-plid-id
                               network-plid-id :network/end] end-id)))))

;; nil on success
(defn add-plan [plans network-type plan-id plan-name net & [corresponding-id]]
  (let [begin (if (= network-type :htn-network)
                :top-hem-network-id
                (:network-id net))
        plan (assoc-if {:plan/plid plan-id
                        :plan/name plan-name
                        :plan/type network-type
                        :plan/begin (composite-key plan-id begin)
                        :plan/networks []}
               :plan/corresponding corresponding-id)]
    (swap! plans
      (fn [st]
        (let [{:keys [plans/plans plan/by-plid
                      network/network-by-plid-id
                      node/node-by-plid-id edge/edge-by-plid-id
                      ]} st
              st-plans (or plans [])
              st-by-plid (or by-plid {})
              st-network-by-plid-id (or network-by-plid-id {})
              st-node-by-plid-id (or node-by-plid-id {})
              st-edge-by-plid-id (or edge-by-plid-id {})
              by-plid {plan-id plan}]
          (assoc st
            :plans/plans (vec (set (conj st-plans plan-id)))
            :plan/by-plid (merge st-by-plid by-plid)
            :network/network-by-plid-id st-network-by-plid-id
            :edge/edge-by-plid-id st-edge-by-plid-id
            :node/node-by-plid-id st-node-by-plid-id
            )
          )))
    ((if (= network-type :htn-network) add-hem-network add-tpn-network)
     plans plan-id begin net)
    nil))

;; look in the TPN
;; pick the begin network
;; look at the edges that are activities AND p-begin c-begin nodes
;;   if one has htn-node then the from is the tpn-node
;;   link that htn-node to that tpn activity or tpn node
(defn link-htn-nodes-to-tpn-nodes [htn-plan tpn-plan]
  (let [{:keys [plan/by-plid network/network-by-plid-id
                edge/edge-by-plid-id node/node-by-plid-id]} @tpn-plan
        tpn-by-plid by-plid
        tpn-network-by-plid-id network-by-plid-id
        tpn-edge-by-plid-id edge-by-plid-id
        tpn-node-by-plid-id node-by-plid-id
        tpn-plan-id (first (keys tpn-by-plid))
        tpn-plan0 (get tpn-by-plid tpn-plan-id)
        {:keys [plan/begin]} tpn-plan0
        tpn-network (get tpn-network-by-plid-id begin)
        {:keys [network/nodes network/edges]} tpn-network
        {:keys [plan/by-plid network/network-by-plid-id
                edge/edge-by-plid-id node/node-by-plid-id]} @htn-plan
        htn-plan-id (first (keys by-plid))]
    (doseq [edge edges]
      (let [edge (get-edge tpn-plan edge)
            {:keys [edge/type edge/id edge/from edge/htn-node]} edge
            edge-id (composite-key tpn-plan-id id)
            htn-node-id (if htn-node (composite-key htn-plan-id htn-node))
            hnode (if htn-node-id (get-node htn-plan htn-node-id))]
        (when (and (= type :activity) htn-node-id (not hnode))
          (println "ERROR: edge" edge-id "specficies htn-node" htn-node "but"
            htn-node-id "is not found"))
        (when (and (= type :activity) htn-node-id hnode)
          (update-edge tpn-plan ;; fully qualify the htn-node
            (assoc edge :edge/htn-node htn-node-id))
          (update-node tpn-plan ;; give the from the htn-node also!
            (assoc (get-node tpn-plan from)
              :node/htn-node htn-node-id))
          (update-node htn-plan ;; backpointer link the htn-node --> edge
            (if (= (:node/type hnode) :htn-expanded-method)
              (assoc hnode
                :node/tpn-selection [[:edge edge-id]])
              (assoc hnode
                :node/tpn-edge edge-id))))))
    (doseq [node nodes]
      (let [node (get-node tpn-plan node)
            {:keys [node/type node/id node/htn-node node/end]} node
            node-id (composite-key tpn-plan-id id)
            htn-node-id (if htn-node (composite-key htn-plan-id htn-node))
            hnode (if htn-node-id (get-node htn-plan htn-node-id))]
        (when (#{:p-begin :c-begin} type)
          ;; create *-end pointer
          (update-node tpn-plan
            (assoc (get-node tpn-plan end) :node/begin node-id))
          (when (and htn-node-id (not hnode))
            (println "ERROR: node" node-id "specficies htn-node" htn-node "but"
              htn-node-id "is not found"))
          (when hnode
            (update-node tpn-plan ;; fully qualify the htn-node
              (assoc node :node/htn-node htn-node-id))
            (update-node htn-plan
              (if (= (:node/type hnode) :htn-expanded-method)
                (assoc hnode
                  :node/tpn-selection [[:node node-id]])
                (assoc hnode
                  :node/tpn-node node-id)))))))))

(defn visit-nodes [tpn-plan node-id prev-visited node-fn]
  (if (prev-visited node-id)
    prev-visited
    (let [node (get-node tpn-plan node-id)
          visited (atom (conj prev-visited node-id))
          tovisit (remove nil? (node-fn node))] ;; visit any nodes returned
      (loop [visit (first tovisit) more (rest tovisit)]
        (when visit
          (swap! visited set/union (visit-nodes tpn-plan visit @visited node-fn))
          (recur (first more) (rest more))))
      @visited)))

;; since we have not precomputed outgoing we'll figure
;; it out the hard way
(defn map-outgoing [tpn-plan node edge-fn]
  (let [node-id (node-key-fn node)
        {:keys [plan/by-plid network/network-by-plid-id
                edge/edge-by-plid-id]} @tpn-plan
        tpn-plan-id (first (keys by-plid))
        tpn-plan0 (get by-plid tpn-plan-id)
        {:keys [plan/begin]} tpn-plan0
        tpn-network (get network-by-plid-id begin)
        {:keys [network/edges]} tpn-network
        from-node (fn [e]
                    (let [edge (get edge-by-plid-id e)]
                      (if (= (:edge/from edge) node-id) edge)))
        outgoing (remove nil? (map from-node edges))]
    (doall (map edge-fn outgoing))))

;; returns end-id
(defn find-end [plan plan-id begin-id]
  (let [the-end (atom :end-not-found)]
    (visit-nodes plan begin-id #{}
      (fn [node]
        (when (= @the-end :end-not-found)
          (reset! the-end (composite-key plan-id (:node/id node)))
          (remove nil?
            (map-outgoing plan node
              (fn [edge]
                (let [{:keys [edge/type edge/to]} edge
                      follow? (#{:activity :null-activity} type)]
                  (if to ;; that was not the end
                    (reset! the-end :end-not-found))
                  (if follow?
                    to))))))))
    @the-end))

(defn is-within? [tpn-plan sel id]
  (let [node (get-node tpn-plan id)
        {:keys [node/end]} node
        sel-id (second sel)
        within (atom (= sel-id end))]
    (if-not @within
      (visit-nodes tpn-plan id #{end}
        (fn [node]
          (if (= sel-id (node-key-fn node))
            (do
              (reset! within true)
              nil)
            (remove nil?
              (map-outgoing tpn-plan node
                (fn [edge]
                  (if (= sel-id (edge-key-fn edge))
                    (do
                      (reset! within true)
                      nil)
                    (let [{:keys [edge/type edge/to]} edge
                          follow? (#{:activity :null-activity} type)]
                      (if follow?
                        to))))))))))
    @within))

;; return the parts of sub in selection
(defn selection-subset [tpn-plan sub selection]
  (loop [a-subs [] a (first sub) a-more (rest sub)]
    (if-not a
      a-subs
      (let [within? (loop [b (first selection) b-more (rest selection)]
                      (if-not b
                        false
                        (if (or (= a b) (and (= (first b) :node)
                                          (is-within? tpn-plan a (second b))))
                          true
                          (recur (first b-more) (rest b-more)))))
            a-subs (if within? (conj a-subs a) a-subs)]
        (recur a-subs (first a-more) (rest a-more))))))

(defn remove-subset [selection remove]
  (loop [selection selection r (first remove) more (rest remove)]
    (if-not r
      selection
      (do
        (recur (vec (filter #(not= % r) selection)) (first more) (rest more))))))

;; return the minimal representation of sel
;; first find any leader nodes among the nodes
;; then add in any non comprised edges
(defn minimal-selection [tpn-plan sel]
  (let [mnodes (vec (remove nil? (filter #(= :node (first %)) sel)))
        medges (remove nil? (filter #(= :edge (first %)) sel))
        msel (loop [msel [] n (first mnodes) more (rest mnodes)]
               (if-not n
                 msel
                 (let [node-subset (selection-subset tpn-plan [n] msel)
                       subset-node (selection-subset tpn-plan msel [n])
                       msel (cond
                              (not (empty? node-subset))
                              msel ;; n is already in msel
                              (not (empty? subset-node))
                              ;; remove the subset-node in msel already, add n
                              (conj (remove-subset msel subset-node) n)
                              :else
                              (conj msel n))]
                   (recur msel (first more) (rest more)))))
        msel (loop [msel msel e (first medges) more (rest medges)]
               (if-not e
                 msel
                 (let [edge-subset (selection-subset tpn-plan [e] msel)
                       msel (cond
                              (not (empty? edge-subset))
                              msel ;; e is already in msel
                                                            :else
                              (conj msel e))]
                   (recur msel (first more) (rest more)))))]
    msel))

;; collect selection comprising
;; -- all tpn-node/tpn-edge from the htn network
;; -- for all child hem's
;;    if they have a tpn-selection, use it
;;    else recurse
(defn update-tpn-selection [htn-plan tpn-plan network-by-plid-id hem-network hem]
  (let [hem-id (node-key-fn hem)
        {:keys [node/htn-network]} hem
        htn-net (get network-by-plid-id htn-network)
        {:keys [network/nodes]} htn-net
        {:keys [network/edges]} hem-network
        selection (loop [selection #{} n (first nodes) more (rest nodes)]
                    (if-not n
                      selection
                      (let [{:keys [node/tpn-edge node/tpn-node]}
                            (get-node htn-plan n)
                            sel (if tpn-edge [:edge tpn-edge]
                                    (if tpn-node [:node tpn-node]))]
                        (recur (if sel (conj selection sel) selection)
                               (first more) (rest more)))))
        selection (loop [selection selection e (first edges) more (rest edges)]
                    (if-not e
                      selection
                      (let [{:keys [edge/from edge/to]} (get-edge htn-plan e)
                            hnode (get-node htn-plan to)
                            {:keys [node/tpn-selection]} hnode
                            tpn-selection (if (= from hem-id)
                                            (if tpn-selection
                                              tpn-selection
                                              (update-tpn-selection
                                                htn-plan tpn-plan
                                                network-by-plid-id
                                                hem-network hnode)))]
                        (recur (if tpn-selection
                                 (set/union selection (set tpn-selection))
                                 selection)
                          (first more) (rest more)))))
        selection (minimal-selection tpn-plan (vec selection))]
    (update-node htn-plan (assoc hem :node/tpn-selection selection))
    ;; (println "DEBUG update-tpn-selection" hem-id "=" selection)
    selection))

(defn complete-tpn-selections [htn-plan tpn-plan]
  (let [{:keys [plan/by-plid network/network-by-plid-id
                edge/edge-by-plid-id node/node-by-plid-id]} @htn-plan
        htn-plid (first (keys by-plid))
        htn-plan0 (get by-plid htn-plid)
        {:keys [plan/begin]} htn-plan0
        hem-network (get network-by-plid-id begin)
        {:keys [network/nodes]} hem-network]
    (doseq [node nodes]
      (let [node (get-node htn-plan node)
            {:keys [node/tpn-selection]} node]
        (when-not tpn-selection
          ;; collect all from htn-network and edges from this hem
          (update-tpn-selection
            htn-plan tpn-plan network-by-plid-id hem-network node))))))

;; returns {:error} map or plans
(defn merge-htn-tpn
  "Merge HTN+TPN"
  {:added "0.1.0"}
  [htn htn-name tpn tpn-name]
  (let [htn-plan (atom {})
        tpn-plan (atom {})
        htn-id (name->id htn-name)
        tpn-id (name->id tpn-name)]
    (or
      (add-plan htn-plan :htn-network htn-id htn-name htn tpn-id)
      (add-plan tpn-plan :tpn-network tpn-id tpn-name tpn htn-id)
      ;; cross link here
      (link-htn-nodes-to-tpn-nodes htn-plan tpn-plan)
      (complete-tpn-selections htn-plan tpn-plan)
      [@htn-plan @tpn-plan])))

;; returns a plan map on success or :error on failure
(defn tpn-plan
  "Parse TPN"
  {:added "0.1.6"}
  [options]
  (let [{:keys [verbose file-format input output]} options
        error (if (or (not (vector? input)) (not= 1 (count input)))
                {:error "input must include exactly one TPN file"})
        error (if (and (not error) #?(:clj false :cljs true))
                {:error "CLJS currently not supported"})
        tpn-filename (if (and (not error)
                           (= 1 (count (filter tpn-filename? input))))
                       (first (filter tpn-filename? input)))
        [error tpn] (if error
                      [error nil]
                      (if (empty? tpn-filename)
                        [{:error (str "TPN file not one of: " input)} nil]
                        (let [rv (parse-tpn {:input tpn-filename :output "-"})]
                          (if (:error rv)
                            [rv nil]
                            [nil rv]))))
        tpn-plan (atom {})
        tpn-name #?(:clj (if-not error (first (fs/split-ext tpn-filename)))
                    :cljs "cljs-not-supported")
        _ (if-not error
            (add-plan tpn-plan :tpn-network (name->id tpn-name) tpn-name tpn))
        out #?(:clj (or error @tpn-plan)
               :cljs {:error "not implemented yet"})]
    (if (stdout-option? output)
      ;; NOTE: this isn't really STDOUT, but simply returns the raw value
      (if (= file-format :json)
        (write-json-str out)
        out)
      #?(:clj (spit output
                (if (= file-format :json)
                  (write-json-str out)
                  (with-out-str (pprint out))))
         :cljs (println "not implemented yet")))))

(defn htn-plan
  "Parse HTN"
  {:added "0.1.6"}
  [options]
  (let [{:keys [verbose file-format input output]} options
        error (if (or (not (vector? input)) (not= 1 (count input)))
                {:error "input must include exactly one HTN file"})
        error (if (and (not error) #?(:clj false :cljs true))
                {:error "CLJS currently not supported"})
        htn-filename (if (and (not error)
                           (= 1 (count (filter htn-filename? input))))
                       (first (filter htn-filename? input)))
        [error htn] (if error
                      [error nil]
                      (if (empty? htn-filename)
                        [{:error (str "HTN file not one of:" input)} nil]
                        (let [rv (parse-htn {:input htn-filename :output "-"})]
                          (if (:error rv)
                            [rv nil]
                            [nil rv]))))
        htn-plan (atom {})
        htn-name #?(:clj (if-not error (first (fs/split-ext htn-filename)))
                    :cljs "cljs-not-supported")
        _ (if-not error
            (add-plan htn-plan :htn-network (name->id htn-name) htn-name htn))
        out #?(:clj (or error @htn-plan)
               :cljs {:error "not implemented yet"})]
    (if (stdout-option? output)
      ;; NOTE: this isn't really STDOUT, but simply returns the raw value
      (if (= file-format :json)
        (write-json-str out)
        out)
      #?(:clj (spit output
                (if (= file-format :json)
                  (write-json-str out)
                  (with-out-str (pprint out))))
         :cljs (println "not implemented yet")))))

(defn merge-networks
  "Merge HTN+TPN inputs"
  {:added "0.1.0"}
  [options]
  (let [{:keys [verbose file-format input output cwd]} options
        error (if (or (not (vector? input)) (not= 2 (count input)))
                "input must include exactly one HTN and one TPN file")
        htn-filename (if (and (not error)
                           (= 1 (count (filter htn-filename? input))))
                       (first (filter htn-filename? input)))
        tpn-filename (if (and (not error)
                           (= 1 (count (filter tpn-filename? input))))
                       (first (filter tpn-filename? input)))
        error (or error
                (if (empty? htn-filename)
                  (str "HTN file not one of: " input)
                  (if (empty? tpn-filename)
                    (str "TPN file not one of: " input))))
        htn-filename (if-not error (validate-input htn-filename cwd))
        error (or error (:error htn-filename))
        tpn-filename (if-not error (validate-input tpn-filename cwd))
        error (or error (:error tpn-filename))
        htn (if (not error) (parse-htn {:input htn-filename :output "-"}))
        error (or error (:error htn))
        tpn (if (not error) (parse-tpn {:input tpn-filename :output "-"}))
        error (or error (:error tpn))
        output (if (and (not (stdout-option? output))
                     (not (string/starts-with? output "/")))
                 (str cwd "/" output)
                 output)
        out
        #?(:clj (if error
                  {:error error}
                  (merge-htn-tpn
                            htn (first (fs/split-ext htn-filename))
                            tpn (first (fs/split-ext tpn-filename))))
           :cljs {:error "not implemented yet"})]
    (if (stdout-option? output)
      ;; NOTE: this isn't really STDOUT, but simply returns the raw value
      (if (= file-format :json)
        (write-json-str out)
        out)
      #?(:clj (spit output
                (if (= file-format :json)
                  (write-json-str out)
                  (with-out-str (pprint out))))
         :cljs (println "not implemented yet")))))
