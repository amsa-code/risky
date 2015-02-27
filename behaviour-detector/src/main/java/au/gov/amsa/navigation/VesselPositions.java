package au.gov.amsa.navigation;

import java.util.concurrent.TimeUnit;

import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;

import com.google.common.base.Optional;

public class VesselPositions {

	private static final long KNOTS_TO_METRES_PER_SECOND = 1852 / TimeUnit.HOURS
			.toSeconds(1);

	public static VesselPosition toVesselPosition(Fix fix) {
		return VesselPosition
				.builder()
				.lat(fix.getLat())
				.lon(fix.getLon())
				.time(fix.getTime())
				.cls(fix.getAisClass() == AisClass.A ? VesselClass.A
						: VesselClass.B)
				.cogDegrees(toDouble(fix.getCourseOverGroundDegrees()))
				.headingDegrees(toDouble(fix.getHeadingDegrees()))
				.speedMetresPerSecond(
						times(fix.getSpeedOverGroundKnots(),
								KNOTS_TO_METRES_PER_SECOND))
				.navigationalStatus(
						fix.getNavigationalStatus().isPresent() ? NavigationalStatus
								.values()[fix.getNavigationalStatus().get()
								.ordinal()] : NavigationalStatus.NOT_DEFINED)
				// build
				.build();
	}

	private static Optional<Double> toDouble(Optional<Float> value) {
		if (!value.isPresent())
			return Optional.absent();
		else
			return Optional.of((double) value.get());
	}

	private static Optional<Double> times(Optional<Float> value, double factor) {
		if (!value.isPresent())
			return Optional.absent();
		else
			return Optional.of((double) value.get() * factor);
	}

}
