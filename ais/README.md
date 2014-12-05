ais
=========

Features:

* parses NMEA 4.0 messages
* parses AIS messages
* RxJava stream utilities for processing AIS data
* saves NMEA streams to disk supplemented with tag block timestamp

Getting started
--------------------
Add this to your pom.xml:
```xml
<dependency>
    <groupId>au.gov.amsa.risky</groupId>
    <artifactId>ais</artifactId>
    <version>${risky.version}</version>
</dependency>
```

AIS Message types
-------------------
Supports these AIS message types:

* Aid To Navigation
* Base Station
* Position A
* Position B
* Position B extended
* Ship Static A

Every other message is classified as ```AisMessageOther```. 

We are very happy to receive PRs with support for extracting other message types!

NmeaSaver
-----------
The obvious format for saving NMEA AIS messages is the raw NMEA message itself supplemented with a 
timestamp in a tag block if required. ```NmeaSaver``` does this for you.

The example below merges the streams from a *satellite* socket broadcast and a *terrestrial* socket
broadcast and then saves the messages in daily files based on arrival timestamp and ensures that 
every message (except for non-first lines of multiline messages) has a tag block with a timestamp (appropriately checksummed).

```java
import java.util.List;
import java.io.File;
import au.gov.amsa.streams.HostPort;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.nmea.saver.FileFactoryPerDay;
import au.gov.amsa.util.nmea.saver.NmeaSaver;
import com.google.common.collect.Lists;
import rx.Observable;

File directory = ...
List<HostPort> hostPorts = Lists.newArrayList(
    HostPort.create("localhost", terrestrialPort, quietTimeoutMs,
					reconnectIntervalMs),
	HostPort.create("localhost", satellitePort, quietTimeoutMs,
					reconnectIntervalMs));
Observable<String> nmea = Strings.mergeLinesFrom(hostPorts);
NmeaSaver saver = new NmeaSaver(nmea, new FileFactoryPerDay(directory));
saver.start();
```

Notes
---------
To analyze timestamped (TAG BLOCK) ais reports in file many.txt:

  mvn clean install exec:java -Dmany=many.txt


