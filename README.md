risky
=====
<a href="https://travis-ci.org/amsa-code/risky"><img src="https://travis-ci.org/amsa-code/risky.svg"/></a><br/>
[![Dependency Status](https://gemnasium.com/com.github.davidmoten/risky.svg)](https://gemnasium.com/com.github.davidmoten/risky)

Tools for analyzing timestamped position data such as vessel position reports from AIS.

Makes extensive use of lazy functional stream programming libraries from [RxJava](https://github.com/reactivex/RxJava).

Status: *pre-alpha*

| Subproject         | Description |
|:-------------------|:------------|
| [ais](ais) | parse nmea and ais messages
| [streams](streams) | read and publish socket broadcasts of string streams (like AIS)
| [behaviour-detector](behaviour-detector) | drift candidate and collision candidate detection algorithms
| [geo-analyzer](geo-analyzer) | distance travelled calculation, traffic density plots
| [craft-analyzer-wms](craft-analyzer-wms) | visualizations using OpenLayers and Grumpy WMS
| [formats](formats) | read and write binary formatted vessel positions and static vessel data

Maven site reports are [here](http://amsa-code.github.io/risky/index.html) including [javadoc](http://amsa-code.github.io/risky/apidocs/index.html).

How to build
----------------
Prerequisite: [Install Java 8 and Maven 3](https://github.com/amsa-code/risky/wiki/Install-Java-and-Maven)

Clone source and build:
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
