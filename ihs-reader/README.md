ihs-reader
==================
Reads ship static data from IHS (LLoyds) that AMSA 
receives monthly as a zip file containing multiple xml files.

How to use
-------------
```java
import au.gov.amsa.ihs.reader.IhsReader;

Observable<Ship> ships = IhsReader.fromZip(file);
```

