package au.gov.amsa.navigation;

import static au.gov.amsa.navigation.DriftDetector.diff;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import au.gov.amsa.navigation.DriftDetector.Options;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import rx.Observable;
import rx.observers.TestSubscriber;

public class DriftDetectorTest {

    private static final double PRECISION = 0.0000001;

    private static final float DRIFT_SPEED_KNOTS = (float) ((DriftDetector.Options.DEFAULT_MIN_DRIFTING_SPEED_KNOTS
            + DriftDetector.Options.DEFAULT_MAX_DRIFTING_SPEED_KNOTS) * 0.5);

    private static Options testOptions = new Options(45, 135, 0.25f, 20f,
            TimeUnit.HOURS.toMillis(4), TimeUnit.MINUTES.toMillis(2));

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
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertTrue(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoCog() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.<Float> empty());
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoHeading() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.<Float> empty());
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testDriftedFalseIfNoSpeed() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.<Float> empty());
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooHigh() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(
                Optional.of(DriftDetector.Options.DEFAULT_MAX_DRIFTING_SPEED_KNOTS * 1.01f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseSpeedTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(110.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(
                Optional.of(DriftDetector.Options.DEFAULT_MIN_DRIFTING_SPEED_KNOTS * 0.99f));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testNotDriftedBecauseCogHeadingDiffTooLow() {
        Fix fix = Mockito.mock(Fix.class);
        Mockito.when(fix.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(fix.headingDegrees()).thenReturn(Optional.of(11.0f));
        Mockito.when(fix.speedOverGroundKnots()).thenReturn(Optional.of(DRIFT_SPEED_KNOTS));
        Mockito.when(fix.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        assertFalse(DriftDetector.isCandidate(Options.instance()).call(fix));
    }

    @Test
    public void testEmpty() {
        List<DriftCandidate> list = getCandidates(Observable.empty());
        assertEquals(0, list.size());
    }

    @Test
    public void testStartingWithNonDriftersDoesNothing() {
        long t = 0;
        // non-drifter
        Fix f1 = createFix(0, DRIFT_SPEED_KNOTS, t);
        // non-drifter
        Fix f2 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // non-drifter
        Fix f3 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRule2TwoDrifters() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2));
        assertEquals(2, list.size());
        assertTrue(f1 == list.get(0).fix());
        assertTrue(f2 == list.get(1).fix());
        assertEquals(f1.time(), list.get(0).driftingSince());
        assertEquals(f1.time(), list.get(1).driftingSince());
    }

    @Test
    public void testRule2TwoDriftersBigTimeGap() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t += TimeUnit.DAYS.toMillis(1));
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRule3ThreeDrifters() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f3 = createFix(92, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3));
        assertEquals(3, list.size());
        assertTrue(f1 == list.get(0).fix());
        assertTrue(f2 == list.get(1).fix());
        assertTrue(f3 == list.get(2).fix());
        assertEquals(f1.time(), list.get(0).driftingSince());
        assertEquals(f1.time(), list.get(1).driftingSince());
        assertEquals(f1.time(), list.get(2).driftingSince());
    }

    @Test
    public void testRule3FourDriftersBigTimeGapBetweenTwoAndThree() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f3 = createFix(92, DRIFT_SPEED_KNOTS, t += testOptions.expiryAgeMs() + 1);
        // drifter
        Fix f4 = createFix(93, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3, f4));
        assertEquals(4, list.size());
        assertTrue(f1 == list.get(0).fix());
        assertTrue(f2 == list.get(1).fix());
        assertTrue(f3 == list.get(2).fix());
        assertTrue(f4 == list.get(3).fix());
        assertEquals(f1.time(), list.get(0).driftingSince());
        assertEquals(f1.time(), list.get(1).driftingSince());
        assertEquals(f3.time(), list.get(2).driftingSince());
        assertEquals(f3.time(), list.get(3).driftingSince());
    }

    @Test
    public void testRule4DrifterThenTwoNonDrifters() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // non-drifter
        Fix f2 = createFix(0, DRIFT_SPEED_KNOTS, t += 1);
        // non-drifter
        Fix f3 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRule5TwoDriftersThenTwoNonDrifters() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        // non-drifter
        Fix f3 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // non-drifter
        Fix f4 = createFix(2, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3, f4));
        assertEquals(2, list.size());
        assertTrue(f1 == list.get(0).fix());
        assertTrue(f2 == list.get(1).fix());
        assertEquals(f1.time(), list.get(0).driftingSince());
        assertEquals(f1.time(), list.get(1).driftingSince());
    }

    @Test
    public void testRule6DrifterNonDrifterDrifter() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // non-drifter
        Fix f2 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f3 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3));
        assertEquals(2, list.size());
        assertTrue(f1 == list.get(0).fix());
        assertTrue(f3 == list.get(1).fix());
        assertEquals(f1.time(), list.get(0).driftingSince());
        assertEquals(f1.time(), list.get(1).driftingSince());
    }

    @Test
    public void testRule6DrifterNonDrifterDrifterOverNonDriftingThreshold() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // non-drifter
        Fix f2 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f3 = createFix(91, DRIFT_SPEED_KNOTS, t += testOptions.nonDriftingThresholdMs() + 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2, f3));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testTwoDriftersButDifferentMmsi() {
        long t = 0;
        // drifter
        Fix f1 = createFix(12344, 90, DRIFT_SPEED_KNOTS, t);
        // drifter
        Fix f2 = createFix(12345, 91, DRIFT_SPEED_KNOTS, t += 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOutOfOrderFixes() {
        long t = 0;
        // drifter
        Fix f1 = createFix(90, DRIFT_SPEED_KNOTS, t);
        // drifter but before the one above
        Fix f2 = createFix(91, DRIFT_SPEED_KNOTS, t -= 1);
        List<DriftCandidate> list = getCandidates(Observable.just(f1, f2));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testBackpressure() {
        TestSubscriber<DriftCandidate> ts = new TestSubscriber<DriftCandidate>(1) {
            @Override
            public void onNext(DriftCandidate t) {
                super.onNext(t);
                request(1);
            }
        };
        long t = 0;
        // non-drifter
        Fix f1 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // non-drifter
        Fix f2 = createFix(1, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f3 = createFix(91, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f4 = createFix(92, DRIFT_SPEED_KNOTS, t += 1);
        // drifter
        Fix f5 = createFix(92, DRIFT_SPEED_KNOTS, t += 1);
        Observable.just(f1, f2, f3, f4, f5).compose(DriftDetector.detectDrift(testOptions))
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertCompleted();
        ts.assertNoErrors();
        assertEquals(3, ts.getOnNextEvents().size());
    }

    private List<DriftCandidate> getCandidates(Observable<Fix> source) {
        return source.compose(DriftDetector.detectDrift(testOptions)).toList().toBlocking()
                .single();
    }

    private static Fix createFix(int mmsi, float courseHeadingDiff, float speedKnots, long time) {
        Fix f = Mockito.mock(Fix.class);
        Mockito.when(f.courseOverGroundDegrees()).thenReturn(Optional.of(10.0f));
        Mockito.when(f.headingDegrees()).thenReturn(Optional.of(10.0f + courseHeadingDiff));
        Mockito.when(f.speedOverGroundKnots()).thenReturn(Optional.of(speedKnots));
        Mockito.when(f.navigationalStatus()).thenReturn(Optional.<NavigationalStatus> empty());
        Mockito.when(f.mmsi()).thenReturn(mmsi);
        Mockito.when(f.fix()).thenReturn(f);
        Mockito.when(f.time()).thenReturn(time);
        return f;
    }

    private static Fix createFix(float courseHeadingDiff, float speedKnots, long time) {
        return createFix(123456789, courseHeadingDiff, speedKnots, time);
    }

}
