geo-analyzer
==============

Features
* Distance travelled calculator
* Produces vessel traffic density plots
* Voyage dataset producer

Voyage dataset producer
---------------------------
The class `VoyageDatasetProducer` is run as a java main method which scans a binary fix collection (sorted files by mmsi, 5 minute downsampled) and
* associates positions with EEZ entry points listed in src/main/resources