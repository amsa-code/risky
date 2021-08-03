package au.gov.amsa.navigation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import au.gov.amsa.navigation.ShipStaticData.Info;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import rx.functions.Func1;

public class CollisionDetectorMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        VesselPosition.validate = true;
        CollisionDetector c = new CollisionDetector();
        // String filename = "/media/an/nmea/2013/NMEA_ITU_20130108.gz";
        Map<Integer, Info> ships = ShipStaticData.getMapFromResource("/ship-data-2014.txt");

        // nmea from file
        // Streams.nmeaFromGzip(filename)
        File file = new File("/media/an/daily-fixes/2014/2014-02-01.fix");
        File candidates = new File(
                "/media/an/temp/" + file.getName() + ".collision-candidates.txt");
        try (PrintStream out = new PrintStream(candidates)) {
            out.println("time,mmsi1,lat1, lon1, cog1, m/s, mmsi2, lat2, lon2, cog2, m/s");
            BinaryFixes.from(file, true, BinaryFixesFormat.WITH_MMSI)
                    .map(VesselPositions.TO_VESSEL_POSITION)
                    .lift(Logging.<VesselPosition> logger().showCount().every(1000).showMemory()
                            .log())
                    // only class A
                    .filter(onlyClassA)
                    // speed must b present
                    .filter(p -> p.speedMetresPerSecond().isPresent())
                    // course must be present
                    .filter(p -> p.cogDegrees().isPresent())
                    // ignore tugs, pilots, towing
                    .filter(p -> {
                        Mmsi mmsi = (Mmsi) p.id();
                        Optional<Info> info = Optional.fromNullable(ships.get(mmsi.value()));
                        return (!info.isPresent() || !info.get().shipType.isPresent()
                                || !isTugPilotTowing(info.get().shipType.get()));
                    })
                    // candidates must both be moving more than N knots
                    .filter(p -> p.speedKnots().get() >= 5)
                    // detect collision candidates
                    .compose(CollisionDetector.detectCollisionCandidates())
                    // filter
                    // .filter(candidatesMovingWithAtLeastSpeedMetresPerSecond(5
                    // * 0.5144444))
                    // log
                    .lift(Logging.<CollisionCandidate> logger().showCount().every(1).showValue()
                            .showMemory().log())
                    .doOnNext(cc -> out.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", cc.time(),
                            cc.position1().id().uniqueId(), cc.position1().lat(),
                            cc.position1().lon(), cc.position1().cogDegrees(),
                            cc.position1().speedMetresPerSecond().map(x -> String.valueOf(x))
                                    .orElse(""),
                            cc.position2().id().uniqueId(), cc.position2().lat(),
                            cc.position2().lon(), cc.position2().cogDegrees(),
                            cc.position2().speedMetresPerSecond().map(x -> String.valueOf(x))
                                    .orElse("")))
                    // count
                    .count()
                    // go
                    .toBlocking().single();
        }
    }

    private static final Set<Integer> tugPilotTowingShipTypes = Sets.newHashSet(31, 32, 50, 53, 52);

    private static boolean isTugPilotTowing(int shipType) {
        return tugPilotTowingShipTypes.contains(shipType);
    }

    private static Func1<CollisionCandidate, Boolean> candidatesMovingWithAtLeastSpeedMetresPerSecond(
            final double minSpeedMetresPerSecond) {
        return c -> c.position1().speedMetresPerSecond().get() >= minSpeedMetresPerSecond
                && c.position2().speedMetresPerSecond().get() >= minSpeedMetresPerSecond;
    }

    private static Func1<VesselPosition, Boolean> onlyClassA = p -> p.cls() == VesselClass.A;

}
