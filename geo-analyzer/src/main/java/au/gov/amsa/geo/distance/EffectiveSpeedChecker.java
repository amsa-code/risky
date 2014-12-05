package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.Util.toPos;

import java.util.concurrent.TimeUnit;

import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.geo.model.SegmentOptions;

import com.google.common.base.Optional;

public class EffectiveSpeedChecker {

	public static boolean effectiveSpeedOk(Fix a, Fix b, SegmentOptions o) {
		Optional<Double> speedKnots = effectiveSpeedKnots(a, b, o);
		return !speedKnots.isPresent() || speedKnots.get() <= o.maxSpeedKnots();
	}

	public static Optional<Double> effectiveSpeedKnots(Fix a, Fix b,
			SegmentOptions o) {

		long timeDiffMs = Math.abs(a.getTime() - b.getTime());

		if (o.acceptAnyFixAfterHours() != null
				&& timeDiffMs >= TimeUnit.HOURS.toMillis(o
						.acceptAnyFixAfterHours())) {
			return Optional.absent();
		} else {
			double distanceBetweenFixesNm = 1.852 * toPos(a).getDistanceToKm(
					toPos(b));
			if (o.maxDistancePerSegmentNm() != null
					&& distanceBetweenFixesNm > o.maxDistancePerSegmentNm())
				return Optional.absent();
			if (distanceBetweenFixesNm > o.speedCheckDistanceThresholdNm()) {
				double timeDiffHoursFloored = (double) Math.max(timeDiffMs,
						o.speedCheckMinTimeDiffMs())
						/ TimeUnit.HOURS.toMillis(1);
				double effectiveSpeedKnots = distanceBetweenFixesNm
						/ timeDiffHoursFloored;
				return Optional.of(effectiveSpeedKnots);
			} else
				return Optional.absent();
		}
	}
}
