Plan schema is and should be the gold model of objects produced by pamela. It should accurately capture the objects, its keys and values produced by pamela. When given a HTN/TPN file, it should perform some rudimentary checks such as:

* nil values for any key. Pamela does not and should not produce objects with nil values.
* In the same vein, there should not be any nil keys either.
* Assuming a set of required keys for each object, report any missing keys.
* Assuming a set of required and optional keys for each object, report any extra keys.
* For all known slots, provide coercion functions. Report any issues with values and leave the values unchanged.

Finally, plan-schema should be used at HTN and TPN generation time to catch any severe errors. i.e It should be used to check generated HTN and TPN in both json and edn format to ensure that they are correct and valid. Given a valid HTN/TPN, the consumers such as planviz and planners need to use plan-schema for coercing json objects.
