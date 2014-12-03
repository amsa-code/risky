risky
=====

<a href="https://travis-ci.org/amsa-code/risky"><img src="https://travis-ci.org/amsa-code/risky.svg"/></a>

Tools for analyzing timestamped position data such as vessel position reports from AIS.

* [streams](streams)

Status: *pre-alpha*

How to build
----------------

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

To release a non-tagged snapshot version to the AMSA internal repository:

```bash
./release-snapshot.sh
```
