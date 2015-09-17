package au.gov.amsa.risky.format;

import com.google.common.base.Optional;

public interface Fix extends HasFix, HasPosition {

    int mmsi();

    long time();

    Optional<NavigationalStatus> navigationalStatus();

    Optional<Float> speedOverGroundKnots();

    Optional<Float> courseOverGroundDegrees();

    Optional<Float> headingDegrees();

    AisClass aisClass();

    Optional<Integer> latencySeconds();

    Optional<Short> source();

    Optional<Byte> rateOfTurn();

}