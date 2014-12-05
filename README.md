risky
=====

<a href="https://travis-ci.org/amsa-code/risky"><img src="https://travis-ci.org/amsa-code/risky.svg"/></a>

Tools for analyzing timestamped position data such as vessel position reports from AIS.

| Subproject         | Description |
|:-------------------|:------------|
| [streams](streams) | read and publish socket broadcasts of string streams (like AIS)
| [ais](ais) | parse nmea and ais messages
| [behaviour-detector](behaviour-detector) | drift candidate and collision candidate detection algorithms (draft)

Status: *pre-alpha*

Maven site reports are [here](http://amsa-code.github.io/risky/index.html) including [javadoc](http://amsa-code.github.io/risky/apidocs/index.html).

How to build
----------------
You need Java 7 or later installed and Maven 3 or later.

```bash
cd <WORKSPACE>
git clone https://github.com/amsa-code/risky.git
cd risky
mvn clean install
```

How to release
---------------
For project maintainers only.

Until we start releasing to Maven Central this is the procedure:

```bash
./release.sh <VERSION>
```

Note that the above command will also deploy the site reports for the new version to [here](http://amsa-code.github.io/risky/index.html).

To release a non-tagged snapshot version to the AMSA internal repository:

```bash
./release-snapshot.sh
```
