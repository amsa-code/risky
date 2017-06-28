geo-analyzer
==============

Features
* Distance travelled calculator
* Produces vessel traffic density plots
* Voyage dataset producer

Voyage dataset producer
---------------------------
The class `VoyageDatasetProducer` is run as a java main method which scans a binary fix collection (sorted files by mmsi, 5 minute downsampled) and
* associates vessel tracks with legs where each leg is comprised of two waypoints being either an EEZ entry/exit point as specified in [eez-waypoints.csv](src/main/resources/eez-waypoints.csv) or a Port from [ports.txt](src/main/resources/ports.txt).