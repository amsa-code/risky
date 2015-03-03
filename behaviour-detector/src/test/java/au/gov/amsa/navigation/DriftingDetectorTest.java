package au.gov.amsa.navigation;

import static au.gov.amsa.navigation.DriftingDetector.KNOTS_TO_METRES_PER_SECOND;
import static au.gov.amsa.navigation.DriftingDetector.diff;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;

public class DriftingDetectorTest {

	private static final double PRECISION = 0.0000001;

	private static final double DRIFT_SPEED_METRES_PER_SECOND = (DriftingDetector.MIN_DRIFTING_SPEED_KNOTS + DriftingDetector.MAX_DRIFTING_SPEED_KNOTS)
	        * 0.5 * KNOTS_TO_METRES_PER_SECOND;

	@Test
	public void testDiff() {
		assertEquals(0, diff(0, 0), PRECISION);
	}

	@Test
	public void testDiff2() {
		assertEquals(10, diff(30, 20), PRECISION);
	}

	@Test
	public void testDiff3() {
		assertEquals(10, diff(20, 30), PRECISION);
	}

	@Test
	public void testDiff4() {
		assertEquals(20, diff(350, 10), PRECISION);
	}

	@Test
	public void testDiff5() {
		assertEquals(20, diff(10, 350), PRECISION);
	}

	@Test
	public void testDrifted() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.of(10.0));
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(110.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DRIFT_SPEED_METRES_PER_SECOND));
		assertTrue(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testDriftedFalseIfNoCog() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.<Double> absent());
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(110.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DRIFT_SPEED_METRES_PER_SECOND));
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testDriftedFalseIfNoHeading() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.of(10.0));
		Mockito.when(p.headingDegrees()).thenReturn(Optional.<Double> absent());
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DRIFT_SPEED_METRES_PER_SECOND));
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testDriftedFalseIfNoSpeed() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.<Double> absent());
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(110.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(Optional.<Double> absent());
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testNotDriftedBecauseSpeedTooHigh() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.of(10.0));
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(110.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DriftingDetector.MAX_DRIFTING_SPEED_KNOTS * 1.01
		                * KNOTS_TO_METRES_PER_SECOND));
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testNotDriftedBecauseSpeedTooLow() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.of(10.0));
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(110.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DriftingDetector.MIN_DRIFTING_SPEED_KNOTS * 0.99
		                * KNOTS_TO_METRES_PER_SECOND));
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}

	@Test
	public void testNotDriftedBecauseCogHeadingDiffTooLow() {
		VesselPosition p = Mockito.mock(VesselPosition.class);
		Mockito.when(p.cogDegrees()).thenReturn(Optional.of(10.0));
		Mockito.when(p.headingDegrees()).thenReturn(Optional.of(11.0));
		Mockito.when(p.speedMetresPerSecond()).thenReturn(
		        Optional.of(DRIFT_SPEED_METRES_PER_SECOND));
		assertFalse(DriftingDetector.IS_CANDIDATE.call(p));
	}
}
