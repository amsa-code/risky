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

    private static Options testOptions = createTestOptions();

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

    @Test
    public void testTwoDriftersBigTimeGapEmitsBoth() {
        Fix fix1 = createFix(100f, DRIFT_SPEED_KNOTS, 0);
        Fix fix2 = createFix(90f, DRIFT_SPEED_KNOTS + 1, TimeUnit.HOURS.toMillis(1));
        List<DriftCandidate> list = getCandidates(Observable.just(fix1, fix2));
        assertEquals(2, list.size());
        Fix r1 = list.get(0).fix();
        Fix r2 = list.get(1).fix();
        assertEquals(fix1.courseOverGroundDegrees(), r1.courseOverGroundDegrees());
        assertEquals(fix1.headingDegrees(), r1.headingDegrees());
        assertEquals(fix2.courseOverGroundDegrees(), r2.courseOverGroundDegrees());
        assertEquals(fix2.headingDegrees(), r2.headingDegrees());
    }

    @Test
    public void testTwoDriftersBigTimeGapThenSmallGapEmitsAll() {
        long t = 0;
        Fix fix1 = createFix(100f, DRIFT_SPEED_KNOTS, t);
        Fix fix2 = createFix(90f, DRIFT_SPEED_KNOTS + 1, t += testOptions.windowSizeMs() * 10);
        Fix fix3 = createFix(95f, DRIFT_SPEED_KNOTS, t += TimeUnit.MILLISECONDS.toMillis(1));
        List<DriftCandidate> list = getCandidates(Observable.just(fix1, fix2, fix3));
        assertEquals(3, list.size());
        Fix r1 = list.get(0).fix();
        Fix r2 = list.get(1).fix();
        Fix r3 = list.get(2).fix();
        assertEquals(fix1.courseOverGroundDegrees(), r1.courseOverGroundDegrees());
        assertEquals(fix1.headingDegrees(), r1.headingDegrees());
        assertEquals(fix2.courseOverGroundDegrees(), r2.courseOverGroundDegrees());
        assertEquals(fix2.headingDegrees(), r2.headingDegrees());
        assertEquals(fix3.courseOverGroundDegrees(), r3.courseOverGroundDegrees());
        assertEquals(fix3.headingDegrees(), r3.headingDegrees());
        assertEquals(fix1.time(), list.get(0).driftingSince());
        assertEquals(fix1.time(), list.get(1).driftingSince());
        assertEquals(fix1.time(), list.get(2).driftingSince());

        assertEquals(3, (int) DriftDetectorOperator.queueSize.get());
    }

    @Test
    public void testTwoDriftersBigTimeGapWithNonDriftBetweenThenSmallGapEmitsAll() {
        long t = 0;
        Fix fix1 = createFix(100f, DRIFT_SPEED_KNOTS, t);
        // non drifter
        Fix fix1a = createFix(0f, DRIFT_SPEED_KNOTS, t + 1);
        Fix fix2 = createFix(90f, DRIFT_SPEED_KNOTS + 1, t += testOptions.windowSizeMs() * 10);
        Fix fix3 = createFix(95f, DRIFT_SPEED_KNOTS, t += TimeUnit.MILLISECONDS.toMillis(1));
        List<DriftCandidate> list = getCandidates(Observable.just(fix1, fix1a, fix2, fix3));
        assertEquals(3, list.size());
        Fix r1 = list.get(0).fix();
        Fix r2 = list.get(1).fix();
        Fix r3 = list.get(2).fix();
        assertEquals(fix1.courseOverGroundDegrees(), r1.courseOverGroundDegrees());
        assertEquals(fix1.headingDegrees(), r1.headingDegrees());
        assertEquals(fix2.courseOverGroundDegrees(), r2.courseOverGroundDegrees());
        assertEquals(fix2.headingDegrees(), r2.headingDegrees());
        assertEquals(fix3.courseOverGroundDegrees(), r3.courseOverGroundDegrees());
        assertEquals(fix3.headingDegrees(), r3.headingDegrees());
        assertEquals(3, (int) DriftDetectorOperator.queueSize.get());
    }

    @Test
    public void testThreeDriftersBigTimeGapThenSmallGapEmitsAllAndQueueDropsFirst() {
        long t = 0;
        Fix fix1 = createFix(100f, DRIFT_SPEED_KNOTS, t);
        Fix fix2 = createFix(90f, DRIFT_SPEED_KNOTS + 1, t += testOptions.windowSizeMs() * 10);
        Fix fix3 = createFix(90f, DRIFT_SPEED_KNOTS + 1, t += testOptions.windowSizeMs() * 10);
        Fix fix4 = createFix(95f, DRIFT_SPEED_KNOTS, t += TimeUnit.MILLISECONDS.toMillis(1));
        List<DriftCandidate> list = getCandidates(Observable.just(fix1, fix2, fix3, fix4));
        assertEquals(4, list.size());
        assertEquals(3, (int) DriftDetectorOperator.queueSize.get());
    }

    private List<DriftCandidate> getCandidates(Observable<Fix> source) {
        return source.compose(DriftDetector.detectDrift(testOptions)).toList().toBlocking()
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
