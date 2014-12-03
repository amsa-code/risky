ais
=========

Features:

* parses NMEA 4.0 messages
* parses AIS messages
* RxJava stream utilities for processing AIS data

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

Notes
---------
To analyze timestamped (TAG BLOCK) ais reports in file many.txt:

  mvn clean install exec:java -Dmany=many.txt


