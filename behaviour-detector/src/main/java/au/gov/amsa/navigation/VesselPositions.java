package au.gov.amsa.navigation;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;
import rx.functions.Func1;

public class VesselPositions {

    private static final double KNOTS_TO_METRES_PER_SECOND = 1852.0 / TimeUnit.HOURS.toSeconds(1);

    // TODO unit test
    public static VesselPosition toVesselPosition(Fix fix, Optional<?> data) {
        return VesselPosition.builder().id(new Mmsi(fix.mmsi())).lat(fix.lat()).lon(fix.lon())
                .time(fix.time()).cls(fix.aisClass() == AisClass.A ? VesselClass.A : VesselClass.B)
                .cogDegrees(toDouble(fix.courseOverGroundDegrees()))
                .headingDegrees(toDouble(fix.headingDegrees()))
                .speedMetresPerSecond(
                        multiply(fix.speedOverGroundKnots(), KNOTS_TO_METRES_PER_SECOND))
                .navigationalStatus(fix.navigationalStatus().isPresent()
                        ? NavigationalStatus.values()[fix.navigationalStatus().get().ordinal()]
                        : NavigationalStatus.NOT_DEFINED)
                .positionAisNmea(Optional.<String> empty())
                .shipStaticAisNmea(Optional.<String> empty())
                // set data
                .data(data)
                // build
                .build();
    }

    public static Func1<HasFix, VesselPosition> TO_VESSEL_POSITION = fix -> toVesselPosition(
            fix.fix(), Optional.empty());

    public static <T extends HasFix> Func1<T, VesselPosition> toVesselPosition(
            final Func1<T, Optional<?>> dataExtractor) {
        return fix -> toVesselPosition(fix.fix(), dataExtractor.call(fix));
    }

    private static Optional<Double> toDouble(Optional<Float> value) {
        if (!value.isPresent())
            return Optional.empty();
        else
            return Optional.of((double) value.get());
    }

    private static Optional<Double> multiply(Optional<Float> value, double factor) {
        if (!value.isPresent())
            return Optional.empty();
        else
            return Optional.of((double) value.get() * factor);
    }

}
