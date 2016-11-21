# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

### [Unreleased]

Changes
* _TBD_

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
[Unreleased]: https://github.com/dollabs/plan-schema/compare/0.2.13...HEAD
