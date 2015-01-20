formats
===========

Java routines for manipulating binary formatted files of vessel positions and static vessel data.

BinaryFixes format (.track)
--------------------------------
All values use *Big Endian* bit order.

| Name         | Type | Bytes | Notes |
|:-------------|:-----|:-----|:-----|
| latitude | float | 4
| longitude | float | 4
| time | long | 8
| navigational status | signed byte | 1 | 127 = not present
| rate of turn | byte | 1 | not used yet
| speed over ground | short | 2 |unit is 1/10 knot, 1023 = not present
| course over ground | short | 2 |unit is 1/10 degree, 3600 = not present
| heading | short | 2 | unit is degrees, 360 = not present
| ais class | byte | 1 | 0 = A, 1 = B

TODO: what about source (e.g. which satellite) and arrival time (so will have record of latency especially for AIS satellite reports)?

How to use in java
---------------------

```java
Observable<Fix> fixes = BinaryFixes.from(new File("target/123456789.track"));
```

How to use with R
----------------------------
To read the above binary format using the R language see [read-binary-fixes.r](src/test/resources/read-binary-fixes.r). 
To test, generate the sample files (see below), then:

```bash
Rscript src/test/resources/read-binary-fixes.r
```

Generate sample files
------------------------
Run unit tests:
```
mvn test
```

This generates these files in the *target* directory:
* ```123456789.track``` - 100,000 identical fixes
* ```123456790.track``` - 2 fixes with different positions and time



