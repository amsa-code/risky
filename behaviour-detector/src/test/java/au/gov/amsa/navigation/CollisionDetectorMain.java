package au.gov.amsa.navigation;

import java.io.File;
import java.io.IOException;

import com.github.davidmoten.rx.slf4j.Logging;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import rx.functions.Func1;

public class CollisionDetectorMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        VesselPosition.validate = true;
        CollisionDetector c = new CollisionDetector();
        String filename = "/media/an/nmea/2013/NMEA_ITU_20130108.gz";
        // nmea from file
        // Streams.nmeaFromGzip(filename)
        BinaryFixes
                .from(new File("/media/an/daily-fixes/2014/2014-02-01.fix"), true,
                        BinaryFixesFormat.WITH_MMSI)
                .map(VesselPositions.TO_VESSEL_POSITION)
                // only class A
                .filter(onlyClassA)
                // lat must be valid
                .filter(p -> p.lat() >= -90 && p.lat() <= 90)
                // lon must be valid
                .filter(p -> p.lon() >= -180 && p.lon() <= 180)
                // speed must b present
                .filter(p -> p.speedMetresPerSecond().isPresent())
                // course must be present
                .filter(p -> p.cogDegrees().isPresent())
                // detect collisions
                .compose(CollisionDetector.detectCollisionCandidates())
                // filter
                .filter(candidatesMovingWithAtLeastSpeedMetresPerSecond(5 * 0.5144444))
                // log
                .lift(Logging.<CollisionCandidate> logger().showCount().every(1).showValue()
                        .showMemory().log())
                // count
                .count()
                // go
                .toBlocking().single();
    }

    private static Func1<CollisionCandidate, Boolean> candidatesMovingWithAtLeastSpeedMetresPerSecond(
            final double minSpeedMetresPerSecond) {
        return c -> c.position1().speedMetresPerSecond().get() >= minSpeedMetresPerSecond
                && c.position2().speedMetresPerSecond().get() >= minSpeedMetresPerSecond;
    }

    private static Func1<VesselPosition, Boolean> onlyClassA = p -> p.cls() == VesselClass.A;

}
