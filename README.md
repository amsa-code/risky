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
For project maintainers, until we start releasing to Maven Central this is the procedure:

```bash
mvn release:prepare
```

This will commit the tagged version you specify. Use the version number only for the tag. Then 

```bash
git checkout <TAG>
mvn clean install 
```

and deploy the jar manually to the internal repository.
