Steps to do nm travelled in eez analysis:

1. copy daily nmea gz files to local directory 
2. Run NmeaGzToBinaryFixesWithMmsiGzMain (modify input and output directories as you see fit) ~6 hours for a year
3. Run BinaryFixesWithMmsiGzCombinedSortMain to move delayed reports to previous day and sort every day ~4 hours for a year
4. Run DistanceTravelledInEezMain ~10 minutes
5. Deliver target/output.csv to client