# plan-schema

Schema validation and coercion utilities for TPNs and HTNs

As this is a pre-release version **plan-schema** has not been
published to [Clojars](https://clojars.org/). You can still clone it and install
it locally.

Check out the [CHANGELOG](CHANGELOG.md)

## Documentation

The **plan-schema** library is part of the [PAMELA](https://github.com/dollabs/pamela) suite of tools.

*TBD* API docs (will be linked from [dollabs.github.io](http://dollabs.github.io/))

## Building

The **plan-schema** library uses [boot](http://boot-clj.com/) as a build tool. For
more on boot see [Sean's blog](http://seancorfield.github.io/blog/2016/02/02/boot-new/) and the [boot Wiki](https://github.com/boot-clj/boot/wiki).

Install [boot](http://boot-clj.com/) if you haven't done so already.

Copy [boot.properties](doc/config/boot.properties) to `~/.boot/boot.properties` (if you haven't customized it yet).

Copy [profile.boot](doc/config/profile.boot) to `~/.boot/boot.properties` (if you haven't customized it yet).

 * Emacs users: when you are ready for interactive development see the comment
   about the `cider-boot` task in [build.boot](build.boot)
 * [Cursive](https://github.com/cursive-ide/cursive) users: use this to
   create a **project.clj** file that Cursive will like.

 ````
boot lein-generate
````

You can install **plan-schema** locally with `boot local`.

You can get help for all available boot tasks with `boot -h`.

## Usage

For convenience you may add the [plan-schema/bin](bin) directory to your `PATH`
(or simply refer to the startup script as `./bin/plan-schema`).

````
tmarble@cerise 242 :) plan-schema --help

plan-schema

Usage: plan-schema [options] action

Options:
  -h, --help                       Print usage
  -V, --version                    Print plan-schema version
  -v, --verbose                    Increase verbosity
  -f, --file-format FORMAT  edn    Output file format
  -i, --input INPUT         ["-"]  Input file(s)
  -o, --output OUTPUT       -      Output file

Actions:
  htn	Parse HTN
  htn-plan	Parse HTN
  merge	Merge HTN+TPN inputs
  tpn	Parse TPN
  tpn-plan	Parse TPN
tmarble@cerise 243 :)
````

*NOTE* the input files used as examples here are *not* part of this git repository.


* Validate TPN JSON

`plan-schema -i ../mission-models-lisp/sept14/tpn.flat.json -o html/examples/sept14/sept14.tpn.json -f json tpn`

* Validate HTN JSON

`plan-schema -i ../mission-models-lisp/sept14/htn.flat.json -o html/examples/sept14/sept14.htn.json -f json htn`

* Validate TPN JSON and coerce to EDN

`plan-schema -i ../mission-models-lisp/sept14/tpn.flat.json -o html/examples/sept14/sept14.tpn.edn -f edn tpn`

* Validate HTN JSON and coerce to EDN

`plan-schema -i ../mission-models-lisp/sept14/htn.flat.json -o html/examples/sept14/sept14.htn.edn -f edn htn`

* Merge TPN and HTN

`plan-schema -i html/examples/sept14/sept14.tpn.edn -i html/examples/sept14/sept14.htn.edn -o html/examples/sept14/sept14.merged.edn -f edn merge`

## Development status and Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) for details on
how to make a contribution.

*NOTE* The tests are (obviously) incomplete!

## Copyright and license

Copyright © 2016 Dynamic Object Language Labs Inc.

Licensed under the [Apache License 2.0](http://opensource.org/licenses/Apache-2.0) [LICENSE](LICENSE)

## Acknowledgement and Disclaimer

This material is based upon work supported by the Army Contracting and
DARPA under contract No. W911NF-15-C-0005.  Any opinions, findings and
conclusions or recommendations expressed in this material are those of
the author(s) and do not necessarily reflect the views of the Army
Contracting Command and DARPA.
