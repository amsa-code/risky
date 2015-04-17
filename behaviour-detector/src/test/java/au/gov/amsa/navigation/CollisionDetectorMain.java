package au.gov.amsa.navigation;

import java.io.IOException;

import rx.functions.Func1;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.ais.AisVesselPositions;

import com.github.davidmoten.rx.slf4j.Logging;

public class CollisionDetectorMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        CollisionDetector c = new CollisionDetector();
        String filename = "/media/analysis/nmea/2013/NMEA_ITU_20130108.gz";
        // nmea from file
        Streams.nmeaFromGzip(filename)
                // do some logging
                .lift(Logging.<String> logger().showCount().showMemory().every(100000).log())
                // extract positions
                .compose(AisVesselPositions.positions())
                // only class A
                .filter(onlyClassA)
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
