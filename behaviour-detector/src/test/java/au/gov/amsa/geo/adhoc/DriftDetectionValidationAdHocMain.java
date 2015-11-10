package au.gov.amsa.geo.adhoc;

import java.io.File;

import au.gov.amsa.ais.rx.Streams;

public class DriftDetectionValidationAdHocMain {

    public static void main(String[] args) {
        Streams.extractFixes(
                Streams.nmeaFromGzip(new File("/media/an/nmea/2014/NMEA_ITU_20140523.gz")))
                .filter(fix -> fix.mmsi() == 538002631).doOnNext(System.out::println).subscribe();
    }

}
