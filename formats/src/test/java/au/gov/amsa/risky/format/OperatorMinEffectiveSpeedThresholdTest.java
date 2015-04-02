package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import au.gov.amsa.risky.format.OperatorMinEffectiveSpeedThreshold.FixWithPreAndPostEffectiveSpeed;

public class OperatorMinEffectiveSpeedThresholdTest {

    @Test
    public void test() {
        long t = 1000;
        long diff = TimeUnit.MINUTES.toMillis(20);
        FixImpl a = createFix(t, 135f);
        t += diff;
        FixImpl b = createFix(t, 135.01f);
        t += diff;
        FixImpl c = createFix(t, 135.02f);
        t += diff;
        FixImpl d = createFix(t, 135.03f);
        t += diff;
        FixImpl e = createFix(t, 135.04f);
        t += TimeUnit.MINUTES.toMillis(15);
        FixImpl f = createFix(t, 135.05f);

        List<FixWithPreAndPostEffectiveSpeed> list =
        // fixes
        Observable.from(Arrays.asList(a, b, c, d, e, f))
        // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                // block and get as a list
                .toList().toBlocking().single();
        for (Object o : list)
            System.out.println(o);
        assertEquals(135.02, list.get(0).fix().lon(), 0.0001);
        assertEquals(1.77, list.get(0).preEffectiveSpeedKnots(), 0.01);
    }

    private FixImpl createFix(long t, float lon) {
        return new FixImpl(213456789, -10f, lon, t, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
    }

    @Test
    public void testOnEmpty() {
        List<FixWithPreAndPostEffectiveSpeed> list = Observable.<HasFix> empty()
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSingle() {
        List<FixWithPreAndPostEffectiveSpeed> list = Observable.just(createFix(0, 135.0f))
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testTwoSmallGapReturnsNothing() {
        List<FixWithPreAndPostEffectiveSpeed> list = Observable
                .just(createFix(0, 135.0f), createFix(100, 135.01f))
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testTwoLargeGapReturnsNothing() {
        List<FixWithPreAndPostEffectiveSpeed> list = Observable
                .just(createFix(0, 135.0f), createFix(TimeUnit.HOURS.toMillis(1), 135.01f))
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testThreeSmallGapThenLargeGapReturnsNothing() {
        List<FixWithPreAndPostEffectiveSpeed> list = Observable
                .just(createFix(0, 135.0f), createFix(100, 135.05f),
                        createFix(TimeUnit.MINUTES.toMillis(60), 135.01f))
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testThreeLargeGapThenLargeGapReturnsSecond() {
        FixImpl a = createFix(0, 135.0f);
        FixImpl b = createFix(TimeUnit.MINUTES.toMillis(31), 135.1f);
        FixImpl c = createFix(TimeUnit.MINUTES.toMillis(63), 135.25f);
        List<FixWithPreAndPostEffectiveSpeed> list = Observable.just(a, b, c)
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertFalse(list.isEmpty());
        assertEquals(b.fix().lon(), list.get(0).fix().lon(), 0.0001);
        FixWithPreAndPostEffectiveSpeed r = list.get(0);
        assertEquals(11.444905004618045, r.preEffectiveSpeedKnots(), 0.00001);
        assertEquals(1.0, r.preError(), 0.001);
        assertEquals(16.6291858282, r.postEffectiveSpeedKnots(), 0.0001);
        assertEquals(2.0, r.postError(), 0.001);
    }

    @Test
    public void testFourLargeGapThenLittleGapThenLargeGapReturnsTwoMiddle() {
        FixImpl a = createFix(0, 135.0f);
        FixImpl b = createFix(TimeUnit.MINUTES.toMillis(31), 135.1f);
        FixImpl c = createFix(TimeUnit.MINUTES.toMillis(32), 135.2f);
        FixImpl d = createFix(TimeUnit.MINUTES.toMillis(62), 135.3f);
        List<FixWithPreAndPostEffectiveSpeed> list = Observable.just(a, b, c, d)
                // aggregate stats
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.MINUTES.toMillis(30)))
                .toList().toBlocking().single();
        assertEquals(2, list.size());
        assertEquals(b.fix().lon(), list.get(0).fix().lon(), 0.0001);
        assertEquals(c.fix().lon(), list.get(1).fix().lon(), 0.0001);
        System.out.println(b);
        System.out.println(c);
    }

}
