package au.gov.amsa.navigation;

import static au.gov.amsa.navigation.DriftDetectorOperator.diff;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import au.gov.amsa.navigation.DriftDetectorOperator.Options;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

import com.google.common.base.Optional;

public class DriftDetectorOperatorTest {

    private static final double PRECISION = 0.0000001;

    private static final float DRIFT_SPEED_KNOTS = (float) ((DriftDetectorOperator.Options.DEFAULT_MIN_DRIFTING_SPEED_KNOTS + DriftDetectorOperator.Options.DEFAULT_MAX_DRIFTING_SPEED_KNOTS) * 0.5);

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
        assertTrue(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoCog() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoHeading() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoSpeed() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.<Float> absent());
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooHigh() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots())
                .thenReturn(
                        Optional.of(DriftDetectorOperator.Options.DEFAULT_MAX_DRIFTING_SPEED_KNOTS * 1.01f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots())
                .thenReturn(
                        Optional.of(DriftDetectorOperator.Options.DEFAULT_MIN_DRIFTING_SPEED_KNOTS * 0.99f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseCogHeadingDiffTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(11.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        assertFalse(DriftDetectorOperator.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testEmpty() {
        List<DriftCandidate> list = getCandidates(Observable.empty());
        assertEquals(0, list.size());
    }

    @Test
    public void testAddingASingleDrifterDoesNotEmitADriftCandidate() {
        Fix fix = createFix(100f, DRIFT_SPEED_KNOTS, 0);
        List<DriftCandidate> list = getCandidates(Observable.just(fix));
        assertEquals(0, list.size());
    }

    private List<DriftCandidate> getCandidates(Observable<Fix> source) {
        return source.compose(DriftDetector.detectDrift(createTestOptions())).toList().toBlocking()
                .single();
    }

    private static Options createTestOptions() {
        return new Options(45, 135, 0.25f, 20f, TimeUnit.MINUTES.toMillis(5), 0, 0.5f,
                TimeUnit.MINUTES.toMillis(2));
    }

    private static Fix createFix(float courseHeadingDiff, float speedKnots, long time) {
        Fix f = Mockito.mock(Fix.class);
        Mockito.when(f.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(f.headingDegrees()).thenReturn(Optional.of(10.0f + courseHeadingDiff));
        Mockito.when(f.speedOverGroundKnots()).thenReturn(Optional.of(speedKnots));
        Mockito.when(f.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> absent());
        Mockito.when(f.mmsi()).thenReturn(123456789L);
        Mockito.when(f.fix()).thenReturn(f);
        Mockito.when(f.time()).thenReturn(time);
        return f;
    }
}
