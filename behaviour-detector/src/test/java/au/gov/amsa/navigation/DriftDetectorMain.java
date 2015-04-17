package au.gov.amsa.navigation;

import java.io.FileNotFoundException;
import java.io.IOException;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.ais.AisVesselPositions;

public class DriftDetectorMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filename = "/media/analysis/nmea/2013/NMEA_ITU_20130108.gz";
        Streams.nmeaFromGzip(filename).compose(AisVesselPositions.positions())
                .compose(DriftDetector.detectDrift())
                // group by mmsi
                .groupBy(f -> f.fix().mmsi())
                // first position
                .flatMap(positions -> positions.first())
                //
                .doOnNext(System.out::println)
                // go
                .subscribe();
    }

}
