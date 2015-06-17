package au.gov.amsa.geo.distance;

import java.util.concurrent.TimeUnit;

import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.util.navigation.Position;

import com.google.common.base.Optional;

public class EffectiveSpeedChecker {

    public static boolean effectiveSpeedOk(Fix a, Fix b, SegmentOptions o) {
        return effectiveSpeedOk(a.getTime(), a.getPosition().getLat(), a.getPosition().getLon(),
                a.getTime(), a.getPosition().getLat(), a.getPosition().getLon(), o);
    }

    public static boolean effectiveSpeedOk(long aTime, double aLat, double aLon, long bTime,
            double bLat, double bLon, SegmentOptions o) {
        Optional<Double> speedKnots = effectiveSpeedKnots(aTime, aLat, aLon, bTime, bLat, bLon, o);
        return !speedKnots.isPresent() || speedKnots.get() <= o.maxSpeedKnots();
    }

    public static Optional<Double> effectiveSpeedKnots(Fix a, Fix b, SegmentOptions o) {
        return effectiveSpeedKnots(a.getTime(), a.getPosition().getLat(), a.getPosition().getLon(),
                a.getTime(), a.getPosition().getLat(), a.getPosition().getLon(), o);
    }

    public static Optional<Double> effectiveSpeedKnots(long aTime, double aLat, double aLon,
            long bTime, double bLat, double bLon, SegmentOptions o) {
        long timeDiffMs = Math.abs(aTime - bTime);

        if (o.acceptAnyFixAfterHours() != null
                && timeDiffMs >= TimeUnit.HOURS.toMillis(o.acceptAnyFixAfterHours())) {
            return Optional.absent();
        } else {
            double distanceBetweenFixesNm = 1.852 * Position.create(aLat, aLon).getDistanceToKm(
                    Position.create(bLat, bLon));
            if (o.maxDistancePerSegmentNm() != null
                    && distanceBetweenFixesNm > o.maxDistancePerSegmentNm())
                return Optional.absent();
            if (distanceBetweenFixesNm > o.speedCheckDistanceThresholdNm()) {
                double timeDiffHoursFloored = (double) Math.max(timeDiffMs,
                        o.speedCheckMinTimeDiffMs())
                        / TimeUnit.HOURS.toMillis(1);
                double effectiveSpeedKnots = distanceBetweenFixesNm / timeDiffHoursFloored;
                return Optional.of(effectiveSpeedKnots);
            } else
                return Optional.absent();
        }
    }
}
