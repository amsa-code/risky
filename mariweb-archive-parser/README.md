
mariweb-archive-parser
=======================

The AIS tool [Mariweb](http://www.imisglobal.com/mariweb/) uses an archive export format that is just mysql scripts for rebuilding its tables and inserting the data.

This project parses such files and converts the information to NMEA 4.0 tag-blocked messages.

To convert mariweb backup files (ITU files containing sql insert statements) into NMEA:

    mvn exec:java -Ddirectory=/media/an/mariweb-2015

This reads all ITU_20*.bu.gz files in the given directory and writes an NMEA_ITU_20*.gz file in the same directory as each ITU file.

For example, `ITU_20150101.bu.gz` is read and `NMEA_ITU_20150101.gz` is produced.

By default the directory is recursively scanned for `ITU_20*.bu.gz` files.

Note that the output line is something like:

```
\c:1325980876,a:1383734682000*16\!ABVDM,1,1,5,B,1E@LR2200Rbrdgqe@Pl68S4H0000,0*39
```

The `c` tag is an NMEA standard timestamp in unix seconds and the `a` tag is our custom tag which is the arrival time of the message in unix ms. Note that the timestamp and the arrival time may differ significantly for satellite ais detections and the existence of the `a` tag allows us to do latency calculations.
