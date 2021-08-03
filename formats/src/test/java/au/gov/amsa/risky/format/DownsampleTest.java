package au.gov.amsa.risky.format;

import static au.gov.amsa.risky.format.Downsample.minTimeStep;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;

public class DownsampleTest {

    @Test
    public void testDownSampleOfEmptyReturnsEmpty() {
        int count = Observable.<Fix> empty().compose(minTimeStep(100, TimeUnit.MILLISECONDS))
                .count().toBlocking().single();
        assertEquals(0, count);
    }

    @Test
    public void testDownSampleOfOneReturnsOne() {
        FixImpl f = createFix(0);
        HasFix fix = Observable.just(f).compose(minTimeStep(100, TimeUnit.MILLISECONDS))
                .toBlocking().single();
        assertEquals(f, fix);
    }

    @Test
    public void testDownSampleOfTwoWithSmallGapReturnsFirst() {
        FixImpl f = createFix(0);
        FixImpl f2 = createFix(50);
        HasFix fix = Observable.just(f, f2).compose(minTimeStep(100, TimeUnit.MILLISECONDS))
                .toBlocking().single();
        assertEquals(f, fix);
    }

    @Test
    public void testDownSampleOfTwoWithBigGapReturnsTwo() {
        FixImpl f = createFix(0);
        FixImpl f2 = createFix(150);
        List<HasFix> fixes = Observable.<HasFix> just(f, f2)
                .compose(minTimeStep(100, TimeUnit.MILLISECONDS)).toList().toBlocking().single();
        assertEquals(Arrays.asList(f, f2), fixes);
    }

    @Test
    public void testDownSampleOfThreeWithSmallGapThenBigGapReturnsOuterTwo() {
        FixImpl f = createFix(0);
        FixImpl f2 = createFix(50);
        FixImpl f3 = createFix(150);
        List<HasFix> fixes = Observable.<HasFix> just(f, f2, f3)
                .compose(minTimeStep(100, TimeUnit.MILLISECONDS)).toList().toBlocking().single();
        assertEquals(Arrays.asList(f, f3), fixes);
    }

    @Test
    public void testDownSampleOfNonZeroGapWithItemsWithSameTimeReturnsFirst() {
        FixImpl f = createFix(50);
        FixImpl f2 = createFix(50);
        FixImpl f3 = createFix(50);
        List<HasFix> fixes = Observable.<HasFix> just(f, f2, f3)
                .compose(minTimeStep(100, TimeUnit.MILLISECONDS)).toList().toBlocking().single();
        assertEquals(Arrays.asList(f), fixes);
    }

    @Test
    public void testDownSampleOf0ForItemsWithSameTimeReturnsAll() {
        FixImpl f = createFix(50);
        FixImpl f2 = createFix(50);
        FixImpl f3 = createFix(50);
        List<HasFix> fixes = Observable.<HasFix> just(f, f2, f3)
                .compose(minTimeStep(0, TimeUnit.MILLISECONDS)).toList().toBlocking().single();
        assertEquals(Arrays.asList(f, f2, f3), fixes);
    }

    private static FixImpl createFix(long time) {
        return new FixImpl(213456789, -10f, 135f, time, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
    }

}
