package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;
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
        Fix a = createFix(t, 135f);
        t += diff;
        Fix b = createFix(t, 135.01f);
        t += diff;
        Fix c = createFix(t, 135.02f);
        t += diff;
        Fix d = createFix(t, 135.03f);
        t += diff;
        Fix e = createFix(t, 135.04f);
        t += TimeUnit.MINUTES.toMillis(15);
        Fix f = createFix(t, 135.05f);

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

    private Fix createFix(long t, float lon) {
        return new Fix(213456789, -10f, lon, t, of(12), of((short) 1),
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
}
