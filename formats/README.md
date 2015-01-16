formats
===========

Java routines for manipulating binary formatted files of vessel positions and static vessel data.

How to use
---------------

```java
Observable<Fix> fixes = BinaryFixes.from(new File("target/123456789.track"));
```

Generate a sample file
-------------------------
To generate a sample of 100,000 identical fixes in the file ```target/123456789.track```:

```
mvn clean install
```



