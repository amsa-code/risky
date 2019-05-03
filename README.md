risky
=====
<a href="https://travis-ci.org/amsa-code/risky"><img src="https://travis-ci.org/amsa-code/risky.svg"/></a><br/>

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

Until we start releasing to Maven Central this is the procedure:

```bash
./release.sh <VERSION>
```
Once you've run the release script got to Jenkins AWS and update the *risky* job, setting **Source Code Management - Branches to build** 
to the latest tag. Run this job manually and the risky artifacts will be deployed locally to the Jenkins AWS server so that they can be used by the parent build.

Note that the above command will also deploy the site reports for the new version to [here](http://amsa-code.github.io/risky/index.html).

To release a non-tagged snapshot version to the AMSA internal repository:

```bash
./release-snapshot.sh
```
