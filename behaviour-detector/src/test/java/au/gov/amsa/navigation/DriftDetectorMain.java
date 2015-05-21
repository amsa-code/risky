package au.gov.amsa.navigation;

import java.io.FileNotFoundException;
import java.io.IOException;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.ais.AisVesselPositions;

import com.github.davidmoten.rx.slf4j.Logging;

public class DriftDetectorMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filename = "/media/an/nmea/2013/NMEA_ITU_20130108.gz";
        Streams.nmeaFromGzip(filename).lift(Logging.<String> logger().every(10000).log())
        //
                .compose(AisVesselPositions.positions()).compose(DriftDetector.detectDrift())
                // group by mmsi
                .groupBy(f -> f.fix().mmsi())
                // first position
                .flatMap(positions -> positions.first())
                //
                .doOnNext(System.out::println)
                //
                .doOnError(System.err::println)
                //
                .count().toBlocking().single();
    }

}
