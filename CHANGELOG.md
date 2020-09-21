# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

### [Unreleased]

Changes
* _TBD_

### [0.3.8] - 2010-09-21

Changes
* Add argsmap declaration for HTNs

### [0.3.7] - 2017-10-04

Changes
* Began some name harmonization work as part of pamela issue 136.

### [0.3.6] - 2017-07-19

Changes
* Implemented sort-map for heterogeneous keys (Closes #37)

### [0.3.5] - 2017-06-24

Changes
* Added coercion support for additional keys: :cost :reward
* Significant improvements to testing macro match-eval-out-err
  - Support Java pattern matching options (e.g. :case-insensitive)
  - Add invert matching option (returns opposite of match result)
  - Prints first form to EVAL on STDOUT (to better track down
    which part of the test passes or fails)
* Added fs-basename and fs-dirname to utils

### [0.3.4] - 2017-06-23

Changes
* Updated test rubrics
* Extended coercion by adding delay-activity-slots-optional
* Default logging messages now sent to STDERR (instead of STDOUT)

### [0.3.3] - 2017-05-11

Changes
* Added coercion support for additional keys: :sequence-end :sequence-label
  :label :between :between-starts :between-ends :display-name :args
* Ensure coerced submaps are also in sorted order
* Split out common functions to a new utils namespace (esp. logging)
* Now the coerce namespace has converted println statements to logging
  (note: the wrapping application should make a calls to initialize
  logging as is done in #'planviz.server/log-initialize)

### [0.3.2] - 2017-05-05

Changes
* Fix bug in merge-htn-tpn (Closes #31)
* Updated dependencies

### [0.3.1] - 2017-04-17

Changes
* Ensure CLI returns same integer exit code whenever calling
  plan-schema.core/plan-schema.
* Added cli.clj test suite for the above.

### [0.3.0] - 2017-04-12

Changes
* Removed CLJS support
  * Closes #28

### [0.2.18] - 2017-04-03

Changes
* Reverted boot-cljs back to "1.7.228-2" to avoid JDK 8 dependency.
  * Closes #26
* Removed superfluous debugging messages
  * Closes PLANVIZ # 80 state end-node

### [0.2.17] - 2017-03-22

Changes
* automated formatting changes.
* Trivial coercion and object checking.
* Helpful function for testing coercion from repl
* Disable verbose output
* impl json reader that converts top level keys to keywords and keys of top level objects only. Closes (21)
* Fixed nil value bug for bounds.
* Fixed Java-ism (Closes #23)

### [0.2.16] - 2017-03-13

Changes
* Type coercion for :edges must be consistently vectors (not sets)
* Ensure that plans are sorted on output
* Fixed test to put temporary/generated files under target/

### [0.2.15] - 2017-02-09

Changes
* Various minor fixes

### [0.2.14] - 2016-11-29

Changes
* Various minor fixes
  - Updated doc/boot.properties and doc/profile.boot
  - Fixed launcher script error when the target/ directory does not exist
  - Removed CIDER references from build.boot
  - Updated dependencies and doc/config/profile.boot example
* Improved TPN and HTN filename detection (Closes PLANVIZ issue 42)

### [0.2.13] - 2016-11-20

Changes
- Fixed HTN TPN merging error in TPN selection sets
- Removed extra debugging statements

### [0.2.12] - 2016-10-26

Changes
- Resolve PLANVIZ issue 30 (cleanup relaxed tpn values)

### [0.2.11] - 2016-10-24

Changes
- Updated schema for recent PAMELA grammar changes
  https://github.com/dollabs/planviz/issues/24
- Removes non-PAMELA slots and attributes from the schema checks
  (which, if found, will fail in --strict mode).
- Improved the plan-schema launcher (will use jar if present)

### [0.2.10] - 2016-10-17

Changes
- Allow plan-schema to be more flexible
  https://github.com/dollabs/planviz/issues/24
- Fixed: Planviz disagrees with Pamela (plan-schema)
  https://github.com/dollabs/pamela/issues/18
- Added command line switch --strict to enforce schema checking
- Expand home in pathnames properly
- Updated dependencies

### 0.2.9 - 2016-09-29

Changes
- Added support for lvar's in temporal-constraints
- Allows state nodes to optionally specify :end-node
- Updated dependencies

### 0.2.7 - 2016-08-01

Changed
- Added support for args and argsmap in TPN's


### 0.2.6 - 2016-07-27

Changed
- Added a slot for nodes and edges called "number" which is a vector
  of integers describing the position of each element in the graph.
  Using this number it is much more performant to determine if one
  element is within the scope of a given node.
- Used the new element numbering to improve the performance
  of merging HTN and TPN plans.
- Updated dependencies.
- Updated commit message

### 0.2.5 - 2016-06-03

Added
* Added delay-actvity TPN-TYPE
* Tolerate unknown TPN objects (for schema evolution)

Changed
* Liberalized flow-characteristics value types
* Updated CLJS dependency
* Command line invocation improvements
  - verbose level 1 will print the expanded command line
* File pathname improvements (cwd now forwarded in options)

### [0.2.4] - 2016-05-25

Added
* Added controllable attribute for activities

### [0.2.3] - 2016-05-16

Added
* Updated dependencies
* Added gh-pages and API docs
* Added PLANVIZ svg diagrams in examples/seattle-2016
* Added support for new constraints: :cost<=-constraint, :reward>=-constraint
* All activities may now have an order

### [0.2.2] - 2016-04-27

Changed
* Updated dependencies
* Ensure relative pathnames are respsected for output files
* Merging plans now creates a top level :htn-expanded-method node

### [0.2.1] - 2016-04-12

Added
* Updated CONTRIBUTING with GPG verified commit info
* Added seattle-2016 TPN and HTN example
* Updated dependencies
* Fixed TPN parsing to ensure each network has a specified end node

### [0.2.0] - 2016-04-12

Changed
* Initial publication on github

### 0.1.16

Added
*  Initial version

[0.2.0]: https://github.com/dollabs/plan-schema/compare/0.1.16...0.2.0
[0.2.1]: https://github.com/dollabs/plan-schema/compare/0.2.0...0.2.1
[0.2.2]: https://github.com/dollabs/plan-schema/compare/0.2.1...0.2.2
[0.2.3]: https://github.com/dollabs/plan-schema/compare/0.2.2...0.2.3
[0.2.4]: https://github.com/dollabs/plan-schema/compare/0.2.3...0.2.4
[0.2.10]: https://github.com/dollabs/plan-schema/compare/0.2.4...0.2.10
[0.2.11]: https://github.com/dollabs/plan-schema/compare/0.2.10...0.2.11
[0.2.12]: https://github.com/dollabs/plan-schema/compare/0.2.11...0.2.12
[0.2.13]: https://github.com/dollabs/plan-schema/compare/0.2.12...0.2.13
[0.2.14]: https://github.com/dollabs/plan-schema/compare/0.2.13...0.2.14
[0.2.15]: https://github.com/dollabs/plan-schema/compare/0.2.14...0.2.15
[0.2.16]: https://github.com/dollabs/plan-schema/compare/0.2.15...0.2.16
[0.2.17]: https://github.com/dollabs/plan-schema/compare/0.2.16...0.2.17
[0.2.18]: https://github.com/dollabs/plan-schema/compare/0.2.17...0.2.18
[0.3.0]: https://github.com/dollabs/plan-schema/compare/0.2.18...0.3.0
[0.3.1]: https://github.com/dollabs/plan-schema/compare/0.3.0...0.3.1
[0.3.2]: https://github.com/dollabs/plan-schema/compare/0.3.1...0.3.2
[0.3.3]: https://github.com/dollabs/plan-schema/compare/0.3.2...0.3.3
[0.3.4]: https://github.com/dollabs/plan-schema/compare/0.3.3...0.3.4
[0.3.5]: https://github.com/dollabs/plan-schema/compare/0.3.4...0.3.5
[0.3.6]: https://github.com/dollabs/plan-schema/compare/0.3.5...0.3.6
[0.3.7]: https://github.com/dollabs/plan-schema/compare/0.3.6...0.3.7
[0.3.7]: https://github.com/dollabs/plan-schema/compare/0.3.7...0.3.8
[Unreleased]: https://github.com/dollabs/plan-schema/compare/0.3.8...HEAD
