package au.gov.amsa.geo.distance;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.navigation.Position;

public class EffectiveSpeedChecker {

    public static boolean effectiveSpeedOk(Fix a, Fix b, SegmentOptions o) {
        return effectiveSpeedOk(a.time(), a.lat(), a.lon(), b.time(), b.lat(), b.lon(), o);
    }

    public static boolean effectiveSpeedOk(long aTime, double aLat, double aLon, long bTime,
            double bLat, double bLon, SegmentOptions o) {
        Optional<Double> speedKnots = effectiveSpeedKnots(aTime, aLat, aLon, bTime, bLat, bLon, o);
        return !speedKnots.isPresent() || speedKnots.get() <= o.maxSpeedKnots();
    }

    public static Optional<Double> effectiveSpeedKnots(Fix a, Fix b, SegmentOptions o) {
        return effectiveSpeedKnots(a.time(), a.lat(), a.lon(), b.time(), b.lat(), b.lon(), o);
    }

    public static Optional<Double> effectiveSpeedKnots(long aTime, double aLat, double aLon,
            long bTime, double bLat, double bLon, SegmentOptions o) {
        long timeDiffMs = Math.abs(aTime - bTime);

        if (o.acceptAnyFixAfterHours() != null
                && timeDiffMs >= TimeUnit.HOURS.toMillis(o.acceptAnyFixAfterHours())) {
            return Optional.empty();
        } else {
            double distanceBetweenFixesNm = Position.create(aLat, aLon)
                    .getDistanceToKm(Position.create(bLat, bLon)) / 1.852;
            if (distanceBetweenFixesNm > o.speedCheckDistanceThresholdNm()) {
                double timeDiffHoursFloored = (double) Math.max(timeDiffMs,
                        o.speedCheckMinTimeDiffMs()) / TimeUnit.HOURS.toMillis(1);
                double effectiveSpeedKnots = distanceBetweenFixesNm / timeDiffHoursFloored;
                return Optional.of(effectiveSpeedKnots);
            } else
                return Optional.empty();
        }
    }
}
