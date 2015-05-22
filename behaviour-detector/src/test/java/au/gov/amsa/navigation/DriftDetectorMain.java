package au.gov.amsa.navigation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.functions.Func1;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.DriftDetectorOperator.Options;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.HasFix;

import com.github.davidmoten.rx.slf4j.Logging;

public class DriftDetectorMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println(Options.instance());
        String filename = "/media/an/nmea/2013/NMEA_ITU_20130108.gz";
        Streams.extractFixes(Streams.nmeaFromGzip(filename))
        // parse positions
        // .compose(AisVesselPositions.positions())
        // log
                .lift(Logging.<HasFix> logger().showCount().every(100000).log())
                // group by mmsi
                .groupBy(f -> f.fix().mmsi())
                //
                .flatMap(
                        o -> o.compose(DriftDetector.detectDrift()).compose(
                                Downsample.minTimeStep(5, TimeUnit.MINUTES, isStartOfDrift())))
                //
                .count()
                //
                .doOnNext(System.out::println)
                //
                .doOnError(System.err::println)
                //
                .subscribe();
    }

    private static Func1<DriftCandidate, Boolean> isStartOfDrift() {
        return c -> c.driftingSince() == c.fix().time();
    }
}
