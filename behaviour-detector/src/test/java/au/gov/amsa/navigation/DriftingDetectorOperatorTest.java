package au.gov.amsa.navigation;

import static au.gov.amsa.navigation.DriftingDetectorOperator.diff;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

import com.google.common.base.Optional;

public class DriftingDetectorOperatorTest {

    private static final double PRECISION = 0.0000001;

    private static final float DRIFT_SPEED_KNOTS = (float) ((DriftingDetectorOperator.MIN_DRIFTING_SPEED_KNOTS + DriftingDetectorOperator.MAX_DRIFTING_SPEED_KNOTS) * 0.5);

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
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertTrue(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testDriftedFalseIfNoCog() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testDriftedFalseIfNoHeading() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testDriftedFalseIfNoSpeed() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooHigh() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(
                Optional.of(DriftingDetectorOperator.MAX_DRIFTING_SPEED_KNOTS * 1.01f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(
                Optional.of(DriftingDetectorOperator.MIN_DRIFTING_SPEED_KNOTS * 0.99f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    @Test
    public void testNotDriftedBecauseCogHeadingDiffTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(11.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftingDetectorOperator.IS_CANDIDATE.call(fix));
    }

    // TODO test when nav status present
}
