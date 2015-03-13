package au.gov.amsa.navigation;

import java.util.concurrent.TimeUnit;

import rx.functions.Func1;
import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;

import com.google.common.base.Optional;

public class VesselPositions {

	private static final double KNOTS_TO_METRES_PER_SECOND = 1852.0 / TimeUnit.HOURS.toSeconds(1);

	// TODO unit test
	public static VesselPosition toVesselPosition(Fix fix, Optional<?> data) {
		return VesselPosition
		        .builder()
		        .id(new Mmsi(fix.getMmsi()))
		        .lat(fix.getLat())
		        .lon(fix.getLon())
		        .time(fix.getTime())
		        .cls(fix.getAisClass() == AisClass.A ? VesselClass.A : VesselClass.B)
		        .cogDegrees(toDouble(fix.getCourseOverGroundDegrees()))
		        .headingDegrees(toDouble(fix.getHeadingDegrees()))
		        .speedMetresPerSecond(
		                multiply(fix.getSpeedOverGroundKnots(), KNOTS_TO_METRES_PER_SECOND))
		        .navigationalStatus(
		                fix.getNavigationalStatus().isPresent() ? NavigationalStatus.values()[fix
		                        .getNavigationalStatus().get().ordinal()]
		                        : NavigationalStatus.NOT_DEFINED)
		        .positionAisNmea(Optional.<String> absent())
		        .shipStaticAisNmea(Optional.<String> absent())
		        // set data
		        .data(data)
		        // build
		        .build();
	}

	public static Func1<HasFix, VesselPosition> TO_VESSEL_POSITION = new Func1<HasFix, VesselPosition>() {
		@Override
		public VesselPosition call(HasFix fix) {
			return toVesselPosition(fix.fix(), Optional.absent());
		}
	};

	public static <T extends HasFix> Func1<T, VesselPosition> toVesselPosition(
	        final Func1<T, Optional<?>> dataExtractor) {
		return new Func1<T, VesselPosition>() {
			@Override
			public VesselPosition call(T fix) {
				return toVesselPosition(fix.fix(), dataExtractor.call(fix));
			}
		};
	}

	private static Optional<Double> toDouble(Optional<Float> value) {
		if (!value.isPresent())
			return Optional.absent();
		else
			return Optional.of((double) value.get());
	}

	private static Optional<Double> multiply(Optional<Float> value, double factor) {
		if (!value.isPresent())
			return Optional.absent();
		else
			return Optional.of((double) value.get() * factor);
	}

}
