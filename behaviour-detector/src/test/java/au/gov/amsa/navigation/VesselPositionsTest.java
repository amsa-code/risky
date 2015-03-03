package au.gov.amsa.navigation;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

public class VesselPositionsTest {

	private static final double PRECISION = 0.00001;

	@Test
	public void testFixSpeedComesThroughCorrectly() {
		Fix fix = new Fix(213456789, -10f, 135f, 12000, of(12), of((short) 1),
		        of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);

		assertEquals(7.5 * 1852.0 / 3600, VesselPositions.TO_VESSEL_POSITION.call(fix)
		        .speedMetresPerSecond().get(), PRECISION);
	}

}
