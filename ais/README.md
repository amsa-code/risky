ais
=========

Features:

* parses NMEA 4.0 messages
* parses AIS messages
* RxJava stream utilities for processing AIS data
* saves NMEA streams to disk supplemented with tag block timestamp

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
TODO mention it here

Notes
---------
To analyze timestamped (TAG BLOCK) ais reports in file many.txt:

  mvn clean install exec:java -Dmany=many.txt


