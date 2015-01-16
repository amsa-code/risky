formats
===========

Java routines for manipulating binary formatted files of vessel positions and static vessel data.

How to use
---------------

```java
Observable<Fix> fixes = BinaryFixes.from(new File("target/123456789.track"));
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


