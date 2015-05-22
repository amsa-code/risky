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


