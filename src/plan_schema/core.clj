;; Copyright © 2016 Dynamic Object Language Labs Inc.
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns plan-schema.core
  "Temporal Planning Network schema utilities"
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [plan-schema.coerce :as records]
            [plan-schema.utils :refer [synopsis strict? fs-basename
                                       error? stdout-option?
                                       read-json-str write-json-str
                                       log-trace log-debug log-info
                                       log-warn log-error]]
            [plan-schema.sorting :refer [sort-map]]
            [clojure.pprint :refer [pprint]]
            [avenir.utils :as au
             :refer [keywordize assoc-if concatv]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as su]
            [schema.spec.core :as spec]
            [me.raynes.fs :as fs]))

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

(defn lvar? [x]
  (and (map? x)
    (#{:lvar
       "lvar"
       "LVAR"} (get x :type))))

(s/defschema eq-lvar?
  "eq-lvar?"
  (s/conditional
    keyword? (s/eq :lvar)
    #(and (string? %)
       (= "lvar" (string/lower-case %))) s/Keyword
    'eq-lvar?))

(s/defschema lvar
  "A temporal constraint"
  {:type eq-lvar?
   :name s/Str
   (s/optional-key :default) bounds})

(def check-lvar (s/checker lvar))

(s/defschema bounds-or-lvar
  "bounds-or-lvar"
  (s/conditional
    vector? bounds
    map? lvar
    'bounds-or-lvar?))

(def check-bounds-or-lvar (s/checker bounds-or-lvar))

(s/defschema args
  "plant function args (positional)"
  [s/Any])

(def check-args (s/checker args))

(s/defschema argsmap
  "plant function args (by parameter name)"
  {s/Keyword s/Any})

(def check-argsmap (s/checker argsmap))

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
   :value bounds-or-lvar
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

(s/defschema element-number
  "Element number"
  [s/Num])

(def check-element-number (s/checker element-number))

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
   (s/optional-key :display-name) s/Str
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :cost) s/Num
   (s/optional-key :reward) s/Num
   (s/optional-key :controllable) s/Bool
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :plant) s/Str
   (s/optional-key :plantid) s/Str
   (s/optional-key :command) s/Str
   (s/optional-key :args) args
   (s/optional-key :argsmap) argsmap
   (s/optional-key :non-primitive) non-primitive
   (s/optional-key :order) s/Num ;; order of activity
   (s/optional-key :number) element-number ;; experimental node/edge number
   s/Keyword s/Any
   })

(def check-activity (s/checker activity))

(defn delay-activity? [x]
  (and (map? x)
    (#{:delay-activity
       "delay-activity"
       "DELAY-ACTIVITY"} (get x :tpn-type))))

(s/defschema eq-delay-activity?
  "eq-delay-activity?"
  (s/conditional
    keyword? (s/eq :delay-activity)
    #(and (string? %)
       (= "delay-activity" (string/lower-case %))) s/Keyword
    'eq-delay-activity?))

(s/defschema delay-activity
  "An delay-activity"
  {:tpn-type eq-delay-activity?
   :uid s/Keyword
   :constraints #{s/Keyword}
   :end-node s/Keyword
   (s/optional-key :name) s/Str
   (s/optional-key :label) s/Keyword ;; label for between
   (s/optional-key :display-name) s/Str
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :cost) s/Num
   (s/optional-key :reward) s/Num
   (s/optional-key :controllable) s/Bool
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :order) s/Num ;; order of delay-activity
   (s/optional-key :number) element-number ;; experimental node/edge number
   })

(def check-delay-activity (s/checker delay-activity))

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
   (s/optional-key :constraints) #{s/Keyword}
   (s/optional-key :label) s/Keyword  ;; label for between
   (s/optional-key :display-name) s/Str
   (s/optional-key :probability) s/Num
   (s/optional-key :cost) s/Num
   (s/optional-key :reward) s/Num
   (s/optional-key :guard) s/Str
   (s/optional-key :order) s/Num ;; order of activity
   (s/optional-key :number) element-number ;; experimental node/edge number
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
   (s/optional-key :display-name) s/Str
   (s/optional-key :args) args
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :htn-node) s/Keyword ;; added by the merge operation
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :number) element-number ;; experimental node/edge number
   (s/optional-key :end-node) s/Keyword
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
   (s/optional-key :display-name) s/Str
   (s/optional-key :args) args
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :probability) s/Num
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :number) element-number ;; experimental node/edge number
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
   (s/optional-key :number) element-number ;; experimental node/edge number
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
   (s/optional-key :display-name) s/Str
   (s/optional-key :args) args
   (s/optional-key :cost<=) s/Num
   (s/optional-key :reward>=) s/Num
   (s/optional-key :sequence-label) s/Keyword ;; label for between
   (s/optional-key :sequence-end) s/Keyword ;; label for between
   (s/optional-key :htn-node) s/Keyword
   ;; htn-node points to htn-primitive-task or htn-expanded-nonprimitive-task
   (s/optional-key :number) element-number ;; experimental node/edge number
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
   (s/optional-key :number) element-number ;; experimental node/edge number
   })

(def check-p-end (s/checker p-end))

;; unknown object is an escape hatch to facilitate future
;; schema evolution

(defn unknown-object? [x]
  (if (strict?)
    false ;; do NOT accept unknown objects
    (if (map? x)
      (do
        (log-warn "ACCEPT" (synopsis x) "UNKNOWN object")
        true)
      false)))

(def known-keywords #{:tpn-type :uid :begin-node :end-node :activities
                      :constraints :incidence-set :label :display-name :args
                      :sequence-label :sequence-end :htn-node})

;; NOTE: this does not work as desired
;; (s/defschema unknown-keyword?
;;   "An unknown keyword"
;;   (s/conditional
;;     #(not (known-keywords (keyword %))) s/Keyword
;;     'unknown-keyword?))

;; NOTE: coerce the possible keys we care about to keywords
(s/defschema unknown-object
  "An unknown object"
  {:tpn-type s/Keyword
   :uid s/Keyword
   ;; (s/optional-key :begin-node) s/Keyword
   ;; (s/optional-key :end-node) s/Keyword
   ;; (s/optional-key :activities) #{s/Keyword}
   ;; (s/optional-key :constraints) #{s/Keyword}
   ;; (s/optional-key :incidence-set) #{s/Keyword}
   ;; (s/optional-key :label) s/Keyword ;; label for between
   ;; (s/optional-key :sequence-label) s/Keyword ;; label for between
   ;; (s/optional-key :sequence-end) s/Keyword ;; label for between
   ;; (s/optional-key :htn-node) s/Keyword
   ;; unknown-keyword? s/Any
   s/Keyword s/Any
   })

(def check-unknown-object (s/checker unknown-object))

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
    delay-activity? delay-activity
    state? state
    c-begin? c-begin
    c-end? c-end
    p-begin? p-begin
    p-end? p-end
    unknown-object? unknown-object
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
   (s/optional-key :display-name) s/Str
   (s/optional-key :args) args
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
   :display-name s/Str
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
   (s/optional-key :edges) [s/Keyword] ;; NOTE: must consistently be a vector
   ;(s/optional-key :parent) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;; NOTE the parent points to the parent htn-network
   ;(s/optional-key :tpn-node) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;(s/optional-key :tpn-edge) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;; tpn-node points to state, c-begin, or p-begin
   s/Keyword s/Any
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
   (s/optional-key :edges) [s/Keyword] ;; NOTE: must consistently be a vector
   ;(s/optional-key :parent) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;; NOTE the parent points to the parent htn-network
   ;(s/optional-key :tpn-node) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;(s/optional-key :tpn-edge) s/Keyword ;; new; 1/27/2017 -- PM -- Not found in data and hence removed from checks.
   ;; tpn-node points to state, c-begin, or p-begin
   s/Keyword s/Any
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

(defn coercion [schema]
  (spec/run-checker
    (fn [s params]
      (let [walk (spec/checker (s/spec s) params)]
        (fn [x]
          (let [result
                (cond
                  (and (string? x)
                    (or (= s s/Keyword) (= s upper-bound)))
                  (walk (keyword (string/lower-case x)))
                  (and (= s #{s/Keyword}) (vector? x))
                  (walk (set x))
                  :else
                  (walk x))]
            (if (su/error? result)
              (if (strict?)
                result
                (let [xstr (synopsis x)
                      explanation (synopsis (s/explain s))
                      errstr (synopsis
                               (with-out-str (print (su/error-val result))))]
                  (log-warn "ACCEPT\n" xstr "\nEXPECTED\n" explanation "ERROR" errstr)
                  x)) ;; return it ANYWAY
              result)))))
    true
    schema))

(def coerce-tpn (coercion tpn))

(def coerce-htn (coercion htn))

;; takes pathname as a string
;;       plantypes as a set of valid plantypes (strings)
;;       formats as a set of valid formats (strings)
;; returns true if filename matches
(defn kind-filename? [pathname plantypes formats]
  (let [basename (fs-basename pathname)
        [format plantype] (reverse (string/split basename #"\."))]
       (boolean (and (plantypes plantype) (formats format)))))

(defn tpn-filename? [filename]
  (kind-filename? filename #{"tpn"} #{"json" "edn"}))

(defn htn-filename? [filename]
  (kind-filename? filename #{"htn"} #{"json" "edn"}))

(defn json-filename? [filename]
  (kind-filename? filename #{"tpn" "htn"} #{"json"}))

(defn edn-filename? [filename]
  (kind-filename? filename #{"tpn" "htn"} #{"edn"}))

(defn validate-input [input cwd]
  (if (fs/exists? input)
    input
    (let [cwd-input (str cwd "/" input)]
      (if (fs/exists? cwd-input)
        cwd-input
        {:error (str "input does not exist: " input)}))))

(defn validate-output [output cwd]
  (if (stdout-option? output)
    output
    (if (string/starts-with? output "/")
      output
      (str cwd "/" output))))

(defn cleanup-relaxed-tpn
  "coerces values of known-keywords to keywords"
  {:added "0.2.0"}
  ([tpn]
   (reduce-kv cleanup-relaxed-tpn {} tpn))
  ([m k v]
   (assoc m k
     (if (map? v)
       (let [kw-as (seq v)]
         (loop [new-v {} kw-a (first kw-as) more (rest kw-as)]
           (if-not kw-a
             new-v
             (let [[kw a] kw-a
                   a (if (#{:activities :constraints :incidence-set} kw)
                       (set (map keyword a))
                       (if (known-keywords kw)
                         (keyword a)
                         a))
                   new-v (assoc new-v kw a)]
               (recur new-v (first more) (rest more))))))
       v))))


;; returns a network map -or- {:error "error message"}
(defn parse-network
  "Parse TPN"
  {:added "0.1.0"}
  [network-type options]
  (let [{:keys [verbose file-format input output cwd]} options
        ;; _ (println "Reading input from:" input)
        verbose? (and (not (nil? verbose)) (pos? verbose))
        input (validate-input (if (vector? input) (first input) input) cwd)
        data (if (:error input) input (slurp input))
        data (if (:error data)
               data
               (if (json-filename? input)
                 (read-json-str data)
                 (read-string data)))
        ;;_ (println "DEBUG DATA\n" (with-out-str (pprint data)))
        result (if (:error data)
                 data
                 (if (= network-type :htn)
                   #_(coerce-htn data)
                   (records/coerce data)
                   #_(coerce-tpn data)
                   (records/coerce data))
                 )
        ;;_ (println "DEBUG RESULT\n" (with-out-str (pprint result)))
        out (if (:error result)
              result
              (if (su/error? result)
                {:error (with-out-str (println (:error result)))}
                (sort-map result)))
        out-json-str (if (= file-format :json)
                       (write-json-str out))
        output (validate-output output cwd)]
    (when (:error out)
      (log-error
        (str "Invalid plan: " input ", see error "
          (if (stdout-option? output) "below " "in ")
          (if-not (stdout-option? output) output))))
    (when-not (stdout-option? output)
      (spit output (or out-json-str ;; JSON here
                     (with-out-str (pprint out))))) ;; EDN here
    (or out-json-str out))) ;; JSON or EDN

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
  (if (keyword? name)
    name
    (keyword (string/replace (string/lower-case name) #"\s+" "_"))))

(defn composite-key [k1 k2]
  (keyword (subs (str k1 k2) 1)))

(defn composite-key? [k]
  (= 2 (count (string/split (name k)  #":"))))

(defn composite-key-fn [k1 k2]
  (fn [props]
    (keyword (subs (str (get props k1) (get props k2)) 1))))

(def node-key-fn (composite-key-fn :plan/plid :node/id))

(def edge-key-fn (composite-key-fn :plan/plid :edge/id))

(def activity-types #{:activity :null-activity :delay-activity})

(defn activity-type? [edge-or-type]
  (activity-types (if (map? edge-or-type)
                    (:edge/type edge-or-type)
                    edge-or-type)))


;; HTN ---------------------

(defn get-node [plan node-id]
  (get-in @plan [:node/node-by-plid-id node-id]))

(defn update-node [plan node]
  ;; (log-debug "UPDATE-NODE" node)
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
        {:keys [end-node label display-name args]} htn-edge
        type :sequence-edge
        to-plid-id (composite-key plan-id end-node)
        edge (assoc-if {:plan/plid plan-id
                        :edge/id edge-id
                        :edge/type type
                        :edge/from from-plid-id
                        :edge/to to-plid-id}
               :edge/label label
               :edge/display-name display-name
               :edge/args args)]
    (update-edge plans edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (add-htn-node plans plan-id network-plid-id end-node net)
    nil))

;; nil on success
(defn add-htn-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        htn-node (get net node-id)
        {:keys [type label display-name args edges]} htn-node
        node (assoc-if {:plan/plid plan-id
                        :node/id node-id
                        :node/type type
                        :node/parent network-plid-id}
               :node/label label
               :node/display-name display-name
               :node/args args)]
    (update-node plans node)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/nodes]
      conj plid-id)
    (when-not (empty? edges)
      (doseq [edge edges]
        ;; workaround schema coercion problem
        (let [edge-id (if (keyword? edge) edge (keyword edge))]
          (add-htn-edge plans plan-id network-plid-id edge-id plid-id net))))
    nil))

;; nil on success
(defn add-htn-network [plans plan-id network-id net]
  (let [htn-network (get net network-id)
        {:keys [type label display-name rootnodes parentid]} htn-network
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
                  :network/display-name display-name
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
        {:keys [end-node edge-type label display-name args order]} hem-edge
        type (if (= edge-type :choice) :choice-edge :parallel-edge)
        to-plid-id (composite-key plan-id end-node)
        edge (assoc-if {:plan/plid plan-id
                        :edge/id edge-id
                        :edge/type type
                        :edge/from from-plid-id
                        :edge/to to-plid-id
                        :edge/order (or order default-order)}
               :edge/label label
               :edge/display-name display-name
               :edge/args args)]
    (update-edge plans edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (add-hem-node plans plan-id network-plid-id end-node net)
    nil))

;; nil on success
(defn add-hem-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        hem-node (get net node-id)
        {:keys [type label display-name args network edges]} hem-node
        ;; HERE we assume at some point in the future edges
        ;; will become a vector (because order is important)
        edges (vec edges)
        plid-network (if network (composite-key plan-id network))
        node (assoc-if {:plan/plid plan-id
                        :node/id node-id
                        :node/type type}
               :node/label label
               :node/display-name display-name
               :node/args args
               :node/htn-network plid-network)]
    (update-node plans node)
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
          {:keys [label display-name rootnodes]} htn-network
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
                        :network/display-name display-name
                        :network/rootnodes plid-rootnodes)
          begin (assoc-if {:plan/plid plan-id
                           :node/id begin-id
                           :node/type :htn-expanded-method
                           :node/htn-network htn-network-id}
                  :node/label label
                  :node/display-name (or display-name "HTN")
                  ;; NOTE: for this "synthetic" top level HEM we don't
                  ;; have any args for the root-task --> they will be
                  ;; in the child HEM of this one.
                  ;; :node/args args
                  )
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
      (update-node plans begin)
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
                       :node/label (:label htn-node)
                       :node/display-name (:display-name htn-node)
                       :node/args (:args htn-node))]
            (update-node plans node)
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
                name label display-name args
                sequence-label sequence-end ;; :activity
                plant plantid command args argsmap non-primitive ;; :activity
                cost reward controllable;; :activity :null-activity
                probability guard ;; :null-activity
                network-flows htn-node order]} net-edge
        to-id (or end-node to-id)
        to-plid-id (composite-key plan-id to-id)
        edge (assoc-if
               (if (nil? controllable) {} {:edge/controllable controllable})
               :plan/plid plan-id
               :edge/id edge-id
               :edge/type tpn-type
               :edge/from from-plid-id
               :edge/to to-plid-id
               :edge/order (or order default-order)
               :edge/value value ;; bounds for temporal constraint
               :edge/between between
               :edge/between-ends between-ends
               :edge/between-starts between-starts
               :edge/name name
               :edge/label label
               :edge/display-name display-name
               :edge/args args
               :edge/sequence-label sequence-label
               :edge/sequence-end sequence-end
               :edge/plant plant
               :edge/plantid plantid
               :edge/command command
               :edge/args args
               :edge/argsmap argsmap
               :edge/cost cost
               :edge/reward reward
               :edge/probability probability
               :edge/guard guard
               :edge/network-flows network-flows
               :edge/non-primitive non-primitive
               :edge/htn-node htn-node)]
    ;; (log-debug "ADDING EDGE" plid-id "TO-ID" to-id "END-NODE" end-node)
    (update-edge plans edge)
    (swap! plans update-in
      [:network/network-by-plid-id network-plid-id :network/edges]
      conj plid-id)
    (when-not (empty? constraints)
      (doseq [constraint constraints]
        (add-tpn-edge plans plan-id network-plid-id constraint
          from-plid-id to-id net)))
    ;; (if (not (keyword? to-id))
    ;;   (log-debug "NOT KEYWORD TO-ID" to-id))
    (add-tpn-node plans plan-id network-plid-id to-id net)
    ;; FIXME handle network-flows non-primitive
    nil))

;; nil on success
(defn add-tpn-node [plans plan-id network-plid-id node-id net]
  (let [plid-id (composite-key plan-id node-id)
        node (get-in @plans [:node/node-by-plid-id plid-id])]
    ;; (log-debug "ADDING NODE?" node-id "NODE" node)
    (when-not node
      ;; :state :c-begin :p-begin :c-end :p-end
      (let [net-node (get net node-id)
            {:keys [tpn-type activities ;; mandatory
                    constraints end-node
                    label display-name args sequence-label sequence-end
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
                   :node/display-name display-name
                   :node/args args
                   :node/sequence-label sequence-label
                   :node/sequence-end sequence-end
                   :node/probability probability
                   :node/htn-node htn-node)]
        ;; (log-debug "ADDING NODE" plid-id "ACTIVITIES" activities
        ;;   "END-NODE" end-node)
        (update-node plans node)
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

(defn inc-number-last [number]
  (let [subgraph (vec (or (butlast number) []))
        n (inc (or (last number) -1))]
    (conj subgraph n)))

;; returns context
(defn set-number [plans plan-id prev-context numbers node? x]
  (if node?
    (let [{:keys [node/id node/type node/begin node/context node/number]} x
          begin? (#{:p-begin :c-begin} type)
          end? (#{:p-end :c-end} type)
          no-context? (nil? context) ;; first pass
          context (if no-context? prev-context context)
          number (if no-context?
                   (get (swap! numbers update-in [context] inc-number-last) context)
                   number)
          node-id (composite-key plan-id id)
          edge-context (if begin?
                         node-id
                         (if end?
                           (:node/context (get-node plans begin))
                           context))]
      (if (and begin? no-context?)
        (swap! numbers assoc edge-context (conj number -1)))
      (when no-context?
        (update-node plans (assoc x :node/context context :node/number number)))
      edge-context)
    (let [number
          (get (swap! numbers update-in [prev-context] inc-number-last)
            prev-context)]
      (update-edge plans (assoc x :edge/number number))
      prev-context)))

(declare visit-nodes)
(declare map-outgoing)

;; add experimental node/edge numbers
;; nil on success
(defn number-nodes-edges [plans plan-id begin-id end-id nodes]
  (let [context ::top
        numbers (atom {context [-1]})]
    (visit-nodes plans begin-id #{end-id}
      (fn [node]
        (let [edge-context (set-number plans plan-id context numbers true node)]
          (remove nil?
            (map-outgoing plans node
              (fn [edge]
                (let [{:keys [edge/type edge/to]} edge
                      follow? (activity-type? type)]
                  (when follow?
                    (set-number plans plan-id edge-context numbers false edge)
                    (set-number plans plan-id edge-context numbers true
                      (get-node plans to))
                    to))))))))
    ;; remove :node/context
    (doseq [node nodes]
      (let [node (get-node plans node)
            plid-id (node-key-fn node)
            ref [:node/node-by-plid-id plid-id]]
        (swap! plans assoc-in ref (dissoc node :node/context))))
    nil))

;; nil on success
(defn add-tpn-network [plans plan-id network-id net]
  (let [net-network (get net network-id)
        {:keys [begin-node end-node]} net-network
        begin-id (composite-key plan-id begin-node)
        end-node (or end-node
                   ;; NOTE: the following will likely MISS the true end
                   ;; if the plan is a top level sequence, beginning with
                   ;; p-begin or c-begin
                   ;; (:end-node (get net begin-node))
                   ::walk)
        end-id (if (= end-node ::walk) end-node (composite-key plan-id end-node))
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
    ;; (if (not (keyword? begin-node))
    ;;   (log-debug "NOT KEYWORD BEGIN-NODE" begin-node))
    (add-tpn-node plans plan-id network-plid-id begin-node net)
    ;; create *-end pointer
    (let [nodes (:network/nodes
                 (get-in @plans [:network/network-by-plid-id network-plid-id]))
          find-end? (= end-node ::walk)] ;; walk the graph to find the end node
      ;; (log-debug "DEBUG NODES =========")
      ;; (log-debug "\n" (with-out-str (pprint nodes)))
      (doseq [node nodes]
        (let [node (get-node plans node)
              {:keys [node/type node/id node/end]} node
              node-id (composite-key plan-id id)
              end-node (if (and (#{:p-begin :c-begin} type) end)
                         (get-node plans end))]
          ;; (log-debug "NODE-ID" node-id "END" end "NODE" node)
          (if-not (empty? end-node)
            (update-node plans (assoc end-node :node/begin node-id))
            ;; (if end
            ;;   (log-debug "END NODE" end "NOT FOUND?"))
            )))
      (when find-end?
        (swap! plans assoc-in [:network/network-by-plid-id
                               network-plid-id :network/end]
          (if find-end?
            (find-end plans plan-id begin-id)
            end-id)))
      (number-nodes-edges plans plan-id begin-id end-id nodes))))

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
;;   (NOTE: include state nodes if superfluous NA's have not been removed)
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
            from-node (get-node tpn-plan from)
            from-htn-node-id (if-let [h (:node/htn-node from-node)]
                               (composite-key htn-plan-id h))
            ;; from-htn-node-id (composite-key tpn-plan from)
            from-htn-node (if from-htn-node-id
                            (get-node htn-plan from-htn-node-id))
            from-htn-node-tpn-selection (or (:node/tpn-selection from-htn-node) [])
            edge-id (composite-key tpn-plan-id id)
            htn-node-id (if htn-node (composite-key htn-plan-id htn-node))
            hnode (if htn-node-id (get-node htn-plan htn-node-id))
            tpn-selection (or (:node/tpn-selection hnode) [])]
        (when (and (or (= type :activity) (= type :delay-activity)) htn-node-id)
          (if (not hnode)
            (log-error "edge" edge-id "specifies htn-node" htn-node "but"
              htn-node-id "is not found")
            (do
              (update-edge tpn-plan ;; fully qualify the htn-node
                (assoc edge :edge/htn-node htn-node-id))
              (when (and from-htn-node-id
                      (or (not= from-htn-node-id htn-node-id)
                        (not (some #(= (second %) edge-id)
                          from-htn-node-tpn-selection))))
                ;; (log-warn "FROM-NODE" from
                ;;   "htn-node will change from" from-htn-node-id
                ;;   "to" htn-node-id) ;; DEBUG
                ;; add this to the htn-node selection before it's lost
                (update-node htn-plan
                  (assoc from-htn-node
                    :node/tpn-selection (conj from-htn-node-tpn-selection
                                          [:edge edge-id]))))
              (update-node tpn-plan ;; give the from the htn-node also!
                (assoc from-node
                  :node/htn-node htn-node-id))
              (update-node htn-plan ;; backpointer link the htn-node --> edge
                (if (= (:node/type hnode) :htn-expanded-method)
                  (assoc hnode
                    :node/tpn-selection (conj tpn-selection [:edge edge-id]))
                  (assoc hnode
                    :node/tpn-edge edge-id))))))))
      (doseq [node nodes]
        (let [node (get-node tpn-plan node)
              {:keys [node/type node/id node/htn-node node/end]} node
              node-id (composite-key tpn-plan-id id)
              from-node? (and htn-node (composite-key? htn-node))
              htn-node-id (if htn-node
                            (if from-node?
                              htn-node
                              (composite-key htn-plan-id htn-node)))
              hnode (if htn-node-id (get-node htn-plan htn-node-id))
              tpn-selection (or (:node/tpn-selection hnode) [])]
          ;; :state for extra nodes when superfluous not removed
          (when (and (not from-node?) ;; b/c activity will have the link
                  (#{:p-begin :c-begin :state} type)
                  htn-node-id)
            (if (not hnode)
              (log-error "node" node-id "specficies htn-node" htn-node
                "but" htn-node-id "is not found")
              (do
                (update-node tpn-plan ;; fully qualify the htn-node
                  (assoc node :node/htn-node htn-node-id))
                (update-node htn-plan
                  (if (= (:node/type hnode) :htn-expanded-method)
                    (assoc hnode
                      :node/tpn-selection (conj tpn-selection [:node node-id]))
                    (assoc hnode
                      :node/tpn-node node-id))))))))))

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
                      follow? (activity-type? type)]
                  (if to ;; that was not the end
                    (reset! the-end :end-not-found))
                  (if follow?
                    to))))))))
    @the-end))

;; new generation using :node/number and :edge/number
;; is sel within node-id?
(defn is-within-node? [tpn-plan sel node-id]
  (let [node (get-node tpn-plan node-id)
        {:keys [node/number]} node
        [element id] sel
        sel-number (if (= :node element)
                     (:node/number (get-node tpn-plan id))
                     (:edge/number (get-edge tpn-plan id)))
        number-n (count number)
        sel-number-n (count sel-number)]
    (and (> sel-number-n number-n)
      (= number
        (vec (take number-n sel-number))))))

;; return the parts of sub in selection
(defn selection-subset [tpn-plan sub selection]
  (loop [a-subs [] a (first sub) a-more (rest sub)]
    (if-not a
      a-subs
      (let [within? (loop [b (first selection) b-more (rest selection)]
                      (if-not b
                        false
                        (if (or (= a b) (and (= (first b) :node)
                                          (is-within-node?
                                            tpn-plan a (second b))))
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
(defn update-tpn-selection [htn-plan tpn-plan network-by-plid-id hem-network
                            hem tpn-selection]
  (let [hem-id (node-key-fn hem)
        {:keys [node/htn-network]} hem
        htn-net (get network-by-plid-id htn-network)
        {:keys [network/nodes]} htn-net
        {:keys [network/edges]} hem-network
        selection (if tpn-selection (set tpn-selection) #{})
        selection (loop [selection selection n (first nodes) more (rest nodes)]
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
                            {:keys [node/tpn-selection
                                    node/tpn-selection-complete?]} hnode
                            tpn-selection (if (= from hem-id)
                                            (if tpn-selection-complete?
                                              tpn-selection
                                              (update-tpn-selection
                                                htn-plan tpn-plan
                                                network-by-plid-id
                                                hem-network hnode
                                                tpn-selection)))]
                        (recur (if tpn-selection
                                 (set/union selection (set tpn-selection))
                                 selection)
                          (first more) (rest more)))))
        ;; _ (log-warn "BEFORE MINIMAL" selection) ;; DEBUG
        selection (minimal-selection tpn-plan (vec selection))]
    (update-node htn-plan
      (assoc hem
        :node/tpn-selection selection
        :node/tpn-selection-complete? true))
    ;; (log-warn "DEBUG update-tpn-selection" hem-id "=" selection) ;; DEBUG
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
            {:keys [node/tpn-selection node/tpn-selection-complete?]} node]
        (when-not tpn-selection-complete?
          ;; collect all from htn-network and edges from this hem
          (update-tpn-selection
            htn-plan tpn-plan network-by-plid-id hem-network
            node tpn-selection))))))

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
      [(sort-map @htn-plan)
       (sort-map @tpn-plan)])))

;; returns a plan map on success or :error on failure
(defn tpn-plan
  "Parse TPN"
  {:added "0.1.6"}
  [options]
  (let [{:keys [verbose file-format input output cwd]} options
        error (if (or (not (vector? input)) (not= 1 (count input)))
                {:error "input must include exactly one TPN file"})
        tpn-filename (if (and (not error)
                           (= 1 (count (filter tpn-filename? input))))
                       (first (filter tpn-filename? input)))
        [error tpn] (if error
                      [error nil]
                      (if (empty? tpn-filename)
                        [{:error
                          (str "Expected a TPN file, but tpn is not in the filename: " input)}
                         nil]
                        (let [rv (parse-tpn {:input tpn-filename :output "-"
                                             :cwd cwd})]
                          (if (:error rv)
                            [rv nil]
                            [nil rv]))))
        tpn-plan (atom {})
        tpn-name (if-not error (first (fs/split-ext tpn-filename)))
        _ (if-not error
            (add-plan tpn-plan :tpn-network (name->id tpn-name) tpn-name tpn))
        out (or error
              (sort-map @tpn-plan))
        out-json-str (if (= file-format :json)
                       (write-json-str out))
        output (validate-output output cwd)]
    (when-not (stdout-option? output)
      (spit output (or out-json-str ;; JSON here
                     (with-out-str (pprint out))))) ;; EDN here
    (or out-json-str out))) ;; JSON or EDN


(defn htn-plan
  "Parse HTN"
  {:added "0.1.6"}
  [options]
  (let [{:keys [verbose file-format input output cwd]} options
        error (if (or (not (vector? input)) (not= 1 (count input)))
                {:error "input must include exactly one HTN file"})
        htn-filename (if (and (not error)
                           (= 1 (count (filter htn-filename? input))))
                       (first (filter htn-filename? input)))
        [error htn] (if error
                      [error nil]
                      (if (empty? htn-filename)
                        [{:error (str "Expected a HTN file, but htn is not in the filename: " input)} nil]
                        (let [rv (parse-htn {:input htn-filename :output "-"
                                             :cwd cwd})]
                          (if (:error rv)
                            [rv nil]
                            [nil rv]))))
        htn-plan (atom {})
        htn-name (if-not error (first (fs/split-ext htn-filename)))
        _ (if-not error
            (add-plan htn-plan :htn-network (name->id htn-name) htn-name htn))
        out (or error
              (sort-map @htn-plan))
        out-json-str (if (= file-format :json)
                       (write-json-str out))
        output (validate-output output cwd)]
    (when-not (stdout-option? output)
      (spit output (or out-json-str ;; JSON here
                     (with-out-str (pprint out))))) ;; EDN here
    (or out-json-str out))) ;; JSON or EDN

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
        htn (if (not error) (parse-htn {:input htn-filename :output "-"
                                        :cwd cwd}))
        error (or error (:error htn))
        tpn (if (not error) (parse-tpn {:input tpn-filename :output "-"
                                        :cwd cwd}))
        ;; _ (log-debug "== TPN begin ==")
        ;; _ (log-debug "\n" (with-out-str (pprint tpn)))
        ;; _ (log-debug "==  TPN end ==")
        error (or error (:error tpn))
        out (if error
              {:error error}
              (merge-htn-tpn
                htn (first (fs/split-ext htn-filename))
                tpn (first (fs/split-ext tpn-filename))))
        out-json-str (if (= file-format :json)
                       (write-json-str out))
        output (validate-output output cwd)]
    (when error
      (log-error
        (str "Invalid plan: " input ", see error "
          (if (stdout-option? output) "below " "in ")
          (if-not (stdout-option? output) output))))
    (when-not (stdout-option? output)
      (spit output (or out-json-str ;; JSON here
                     (with-out-str (pprint out))))) ;; EDN here
    (or out-json-str out))) ;; JSON or EDN
