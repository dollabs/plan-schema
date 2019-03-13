# plan-schema

Schema validation and coercion utilities for TPNs and HTNs

As this is a pre-release version **plan-schema** has not been
published to [Clojars](https://clojars.org/). You can still clone it and install
it locally.

Check out the [CHANGELOG](CHANGELOG.md)

## Documentation

The **plan-schema** library is part of the [PAMELA](https://github.com/dollabs/pamela) suite of tools.

See the [API docs](http://dollabs.github.io/plan-schema/doc/api/)

## Building

The **plan-schema** library uses [boot](http://boot-clj.com/) as a build tool. For
more on boot see [Sean's blog](http://seancorfield.github.io/blog/2016/02/02/boot-new/) and the [boot Wiki](https://github.com/boot-clj/boot/wiki).

Install [boot](http://boot-clj.com/) if you haven't done so already.

Copy [boot.properties](doc/config/boot.properties) to `~/.boot/boot.properties` (if you haven't customized it yet). *NOTE*: **plan-schema** *requires* Clojure 1.8.0 or later (`BOOT_CLOJURE_VERSION=1.8.0`)

Copy [profile.boot](doc/config/profile.boot) to `~/.boot/profile.boot` (if you haven't customized it yet).

 * Emacs users: when you are ready for interactive development see the comment
   about the `cider-boot` task in [build.boot](build.boot)
 * [Cursive](https://github.com/cursive-ide/cursive) users: use this to
   create a **project.clj** file that Cursive will like.

 ````
boot lein-generate
````

In order to use the **plan-schema** with other programs
(e.g. [planviz](https://github.com/dollabs/planviz))
you need to install it in your local repository
(i.e. where Maven puts files, usually `~/.m2`):

````
boot local
````


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


* Validate TPN JSON

`plan-schema -i examples/seattle-2016/seattle.tpn.json -o seattle.tpn.json -f json tpn`

* Validate HTN JSON

`plan-schema -i examples/seattle-2016/seattle.htn.json -o seattle.htn.json -f json htn`

* Validate TPN JSON and coerce to EDN

`plan-schema -i examples/seattle-2016/seattle.tpn.json -o examples/seattle-2016/seattle.tpn.edn -f edn tpn`

* Validate HTN JSON and coerce to EDN

`plan-schema -i examples/seattle-2016/seattle.htn.json -o examples/seattle-2016/seattle.htn.edn -f edn htn`

* Merge TPN and HTN

`plan-schema -i examples/seattle-2016/seattle.htn.edn -i examples/seattle-2016/seattle.tpn.edn -o examples/seattle-2016/seattle.merged.edn -f edn merge`

## Development status and Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) for details on
how to make a contribution.

*NOTE* The tests are (obviously) incomplete!

## Copyright and license

Copyright © 2016 Dynamic Object Language Labs Inc.

Licensed under the [Apache License 2.0](http://opensource.org/licenses/Apache-2.0) [LICENSE](LICENSE)

## Acknowledgement and Disclaimer

This work was supported by Contract FA8650-11-C-7191 with the US
Defense Advanced Research Projects Agency (DARPA) and the Air Force
Research Laboratory.  The views expressed are those of the authors and
do not reflect the official policy or position of the Department of
Defense or the U.S. Government.
