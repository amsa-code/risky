ais
=========

Features:

* parses NMEA 4.0 messages
* parses AIS messages
* [RxJava](https://github.com/ReactiveX/RxJava) stream utilities for processing AIS data
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

How to read AisMessage objects from a zipped nmea file
----------------------------------------------------------

```java
import au.gov.amsa.ais.rx.Streams;

File file = new File("/media/an/nmea/2015/NMEA_ITU_20150521.gz");
Streams.nmeaFromGzip(file)
       .compose(o -> Streams.extract(o))
       .forEach(System.out::println);
```

will print out

```
message=Optional.of(Timestamped [time=1432130399000, message=AisPositionA [source=null, messageId=1, repeatIndicator=0, mmsi=503783000, navigationalStatus=UNDER_WAY_USING_ENGINE, rateOfTurn=-127, speedOverGroundKnots=4.0, isHighAccuracyPosition=true, longitude=151.78541666666666, latitude=-32.92156, courseOverGround=82.8, trueHeading=55, timeSecondsOnly=0, specialManoeuvreIndicator=0, spare=0, isUsingRAIM=true, communications=Communications [startIndex=149, syncState=0, slotTimeout=1, receivedStations=null, slotNumber=null, hourUtc=0, minuteUtc=0, slotOffset=null]]]), line=\c:1432130399*5C\!ABVDM,1,1,,A,17PLNF0P@`bnlHUe:H63?1f02400,0*4E
message=Optional.of(Timestamped [time=1432212545000, message=AisShipStaticA [source=null, messageId=5, repeatIndicator=0, mmsi=566749000, aisVersionIndicator=0, imo=Optional.of(9610987), callsign=9V9719, name=GLOVIS MAESTRO, dimensionA=Optional.of(161), dimensionB=Optional.of(30), dimensionC=Optional.of(12), dimensionD=Optional.of(20), typeOfElectronicPositionFixingDevice=0, expectedTimeOfArrival=1432375200000, expectedTimeOfArrivalUnprocessed=375424, maximumPresentStaticDraughtMetres=78.0, destination=GEELONG, dataTerminalAvailable=true, spare=0, shipType=70]]), line=\c:1432212545,g:1-1-6*1F\!BSVDM,1,1,6,B,58LOWB02BafgUKWO7V0LhuHU>0l4E=A8v2222216D8N<D1Kb0CQiAC3kQp8888888888880,0*6C
...
```
How to read fixes from a zipped nmea file
-------------------------------------------
```java
File file = new File("/media/an/nmea/2015/NMEA_ITU_20150521.gz");
Streams.nmeaFromGzip(file)
       .compose(o -> Streams.extractFixes(o))
       .forEach(System.out::println);
```

will print out 
```
Fix [mmsi=503472000, lat=-20.31346, lon=118.573654, time=1432130924000, navigationalStatus=Optional.of(UNDER_WAY_USING_ENGINE), speedOverGroundKnots=Optional.of(0.0), courseOverGroundDegrees=Optional.of(349.9), headingDegrees=Optional.of(123.0), aisClass=A, latencySeconds=Optional.absent(), source=Optional.absent()]
Fix [mmsi=503250800, lat=-32.062496, lon=115.74797, time=1432130924000, navigationalStatus=Optional.of(UNDER_WAY_USING_ENGINE), speedOverGroundKnots=Optional.of(0.0), courseOverGroundDegrees=Optional.of(0.1), headingDegrees=Optional.absent(), aisClass=A, latencySeconds=Optional.absent(), source=Optional.absent()]
...
```
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

Extract BinaryFixes (.track) from NMEA zip files
-------------------------------------------------
The command below will create binary format versions of the NMEA files under *target/binary* and recurses the 
input directory */media/analysis/test* processing all files matching the pattern ```NMEA_ITU_20.*.gz```. 

```bash
mvn exec:java -P bin -Dinput=/media/an/nmea -Doutput=/media/an/binary-fixes-all -Dpattern='NMEA_ITU_20.*.gz' -Dby=month|year
```
Using 6 cores on a Xeon E5-1650@3.20GHz, the above command extracted and wrote 252K fixes/second. 
```input``` and ```output``` were different directories on a single SSD. Conversion of a year's worth of AMSA AIS NMEA lines 
takes 2 hours.

Effect of downsampling AIS
-------------------------------

one day of AIS reports in BinaryFix format unzipped is 2.7MB, 12.8MB, 234MB for downsampling of 
1 hour, 5 minutes, none, respectively. 
For a year that equals 1GB, 4.7GB and 85GB respectively.

Notes
---------
To analyze timestamped (TAG BLOCK) ais reports in file many.txt:

```mvn clean install exec:java -Dmany=many.txt```

AIS Ship Types
-----------------
This is the decode of the ship type numbers from AIS:

```
10,Reserved - All
11,Reserved - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
12,Reserved - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
13,Reserved - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
14,Reserved - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
15,Reserved - Reserved 5
16,Reserved - Reserved 6
17,Reserved - Reserved 7
18,Reserved - Reserved 8
19,Reserved - No additional info
20,WIG - All
21,WIG - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
22,WIG - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
23,WIG - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
24,WIG - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
25,WIG - Reserved 5
26,WIG - Reserved 6
27,WIG - Reserved 7
28,WIG - Reserved 8
29,WIG - No additional info
30,Fishing
31,Towing
32,Towing Long/Large
33,Engaged in dredging or underwater operations
34,Engaged in diving operations
35,Engaged in military operations
36,Sailing
37,Pleasure craft
38,Reserved
39,Reserved
40,HSC - All
41,HSC - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
42,HSC - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
43,HSC - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
44,HSC - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
45,HSC - Reserved 5
46,HSC - Reserved 6
47,HSC - Reserved 7
48,HSC - Reserved 8
49,HSC - No additional info
50,Pilot vessel
51,SAR
52,Tug
53,Port tender
54,Vessel with anti-pollution facilities or equipment
55,Law enforcement
56,Local 56
57,Local 57
58,Medical transport
59,Ship according to RR Resolution No. 18 (Mob-83)
60,Passenger ship - All
61,Passenger ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
62,Passenger ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
63,Passenger ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
64,Passenger ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
65,Passenger ship - Reserved 5
66,Passenger ship - Reserved 6
67,Passenger ship - Reserved 7
68,Passenger ship - Reserved 8
69,Passenger ship - No additional info
70,Cargo ship - All
71,Cargo ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
72,Cargo ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
73,Cargo ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
74,Cargo ship - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
75,Cargo ship - Reserved 5
76,Cargo ship - Reserved 6
77,Cargo ship - Reserved 7
78,Cargo ship - Reserved 8
79,Cargo ship - No additional info
80,Tanker - All
81,Tanker - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
82,Tanker - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
83,Tanker - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
84,Tanker - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
85,Tanker - Reserved 5
86,Tanker - Reserved 6
87,Tanker - Reserved 7
88,Tanker - Reserved 8
89,Tanker - No additional info
90,Other - All
91,Other - Carrying DG, HS, or MP, IMO Hazard or pollutant category A
92,Other - Carrying DG, HS, or MP, IMO Hazard or pollutant category B
93,Other - Carrying DG, HS, or MP, IMO Hazard or pollutant category C
94,Other - Carrying DG, HS, or MP, IMO Hazard or pollutant category D
95,Other - Reserved 5
96,Other - Reserved 6
97,Other - Reserved 7
98,Other - Reserved 8
99,Other - No additional info
```


