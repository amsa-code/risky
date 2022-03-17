geo-analyzer
==============

Features
* Distance travelled calculator
* Produces vessel traffic density plots
* Voyage dataset producer

## Voyage dataset producer
The class `VoyageDatasetProducer` is run as a java main method which scans a binary fix collection (sorted files by mmsi, 5 minute downsampled):
* associates vessel tracks with legs where each leg is comprised of two waypoints being either an EEZ entry/exit point as specified in [eez-waypoints.csv](src/main/resources/eez-waypoints.csv) or a Port from [ports.txt](src/main/resources/ports.txt).
* data is written in csv format to file `target/legs.txt`
* Legs comprised solely of EEZ entry/exit points are excluded
* also writes binary track files (one for all vessels)
* expects input sorted by id (mmsi) and time

### Update 17 Mar 2022
`VoyageDatasetProducer` consumes binary fixes as input. 

`VoyageDatasetProducer2` consumes delimited text that is been formatted to enable sorting by mmsi, time just by doing a line sort.

The oracle extraction sql is this:

```
select case mmsi when null then imo else mmsi end mmsi, time, lat, lon 
from 
(select
  craft_type_id,  
  lat, 
  lon, 
  to_char(position_time, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') time,
  mmsi, 
  imo
from cts.position 
where position_time >= to_date('2017-01-01', 'YYYY-MM-DD')) 
where craft_type_id = 1 and (imo is not null or mmsi is not null);
```

Note that it may seem that the construction of the sql might be more compact (just one select for instance) but
the way it is structured does the right thing with the time index so that a full scan is not required.

The file it produces can be sorted then via the command
```bash
sort export.txt >export.sorted.txt
```

The file `export.sorted.txt` is suitable then for processing by `VoyageDatasetProducer2` (edit the main method to set the input filename).
