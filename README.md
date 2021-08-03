risky
=====
<a href="https://github.com/amsa-code/risky/actions/workflows/ci.yml"><img src="https://github.com/amsa-code/risky/actions/workflows/ci.yml/badge.svg"/></a><br/>
[![codecov](https://codecov.io/gh/amsa-code/risky/branch/master/graph/badge.svg)](https://codecov.io/gh/amsa-code/risky)<br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/risky/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/au.gov.amsa/risky)<br/>

Tools for analyzing timestamped position data such as vessel position reports from AIS.

Makes extensive use of lazy functional stream programming libraries from [RxJava](https://github.com/reactivex/RxJava).

Requires Java 8.

Status: *in production*

| Subproject         | Description |
|:-------------------|:------------|
| [ais](ais) | parse nmea and ais messages
| [streams](streams) | read and publish socket broadcasts of string streams (like AIS)
| [behaviour-detector](behaviour-detector) | drift candidate and collision candidate detection algorithms
| [geo-analyzer](geo-analyzer) | distance travelled calculation, traffic density plots
| [craft-analyzer-wms](craft-analyzer-wms) | visualizations using OpenLayers and Grumpy WMS
| [formats](formats) | read and write binary formatted vessel positions and static vessel data
| [ihs-reader](ihs-reader) | read static ship information from [IHS LLoyds](http://www.shipfinder.org/) sourced zipped xml files

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

This the procedure to install a new version to our internal repository.  

```bash
./release.sh <VERSION>
```
The script above pushes a tagged version to github. Go to the project Releases and publish the release for the tag you just made. This will initiate a GitHub Action that deploys 
the jars to Maven Central.

To release a non-tagged snapshot version to the AMSA internal repository:

```bash
./release-snapshot.sh
```
