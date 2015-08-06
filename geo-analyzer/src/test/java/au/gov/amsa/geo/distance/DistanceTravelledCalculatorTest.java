package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.Util.toPos;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.assertEquals;
import static rx.Observable.from;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.geo.model.Position;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;
import au.gov.amsa.util.navigation.Position.LongitudePair;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

public class DistanceTravelledCalculatorTest {

    private static final Fix f1 = new FixImpl(1, -35.0f, 142.0f, 0, AisClass.A);
    private static final Fix f3 = new FixImpl(3, -35.12f, 142.12f, HOURS.toMillis(2), AisClass.A);
    private static final Fix f4 = new FixImpl(4, -36.12f, 143.12f, HOURS.toMillis(2), AisClass.A);

    @Test
    public void testPairs() {
        System.out.println(from(asList(1, 2, 3)).buffer(2, 1).toList().toBlocking().single());
    }

    @Test
    public void testWithFiles() {

        long t = System.currentTimeMillis();
        Observable<File> files = from(
                new File("src/test/resources/fixes").listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        return file.getName().startsWith("503.*track");
                    }
                }));
        final AtomicInteger count = new AtomicInteger();
        DistanceCalculationMetrics metrics = new DistanceCalculationMetrics();
        new DistanceTravelledCalculator(Options.builder().originLat(0).originLon(0)
                .cellSizeDegrees(0.1).bounds(new Bounds(0, 120, -60, 175)).build(), metrics)
                        .calculateDistanceByCellFromFiles(files).toBlocking()
                        .forEach(new Action1<CellAndDistance>() {

                            @Override
                            public void call(CellAndDistance cell) {
                                System.out.println(cell);
                                count.incrementAndGet();
                            }
                        });
        System.out.println(
                count.get() + " cells returned in " + (System.currentTimeMillis() - t) + "ms");
    }

    @Test
    public void testGetCellDistances() {

        Options options = Options.builder().originLat(0).originLon(0).cellSizeDegrees(0.1)
                .bounds(new Bounds(0, 100, -60, 175)).build();
        Observable<CellAndDistance> list = DistanceTravelledCalculator.getCellDistances(f1, f3,
                options);
        System.out.println(list.toList().toBlocking().single());

    }

    @Test
    public void testGetCellDistancesEqualSingleLegGreatCircleDistanceAlmost() {

        Options options = Options.builder()
                // setup origin
                .originLat(0).originLon(0)
                // set cell size
                .cellSizeDegrees(1)
                // set bounds
                .bounds(new Bounds(0, 100, -60, 175))
                // build options
                .build();
        Observable<CellAndDistance> list = DistanceTravelledCalculator.getCellDistances(f1, f4,
                options);
        System.out.println("totalBearing=" + toPos(f1).getBearingDegrees(toPos(f4)));

        double totalNm = 0;
        for (CellAndDistance value : list.toList().toBlocking().single()) {
            System.out.println(value);
            totalNm += value.getDistanceNm();
        }
        assertEquals(toPos(f1).getDistanceToKm(toPos(f4)) / 1.852, totalNm, 0.00001);
    }

    @Test
    public void manualTestCase() {
        Position a = new Position(-30.8, 140.2);
        Position b = new Position(-31.7, 142.6);
        System.out.println(toPos(a).getBearingDegrees(toPos(b)));
        Options options = Options.builder()
                // setup origin
                .originLat(0).originLon(0)
                // set cell size
                .cellSizeDegrees(1.0)
                // set bounds
                .bounds(new Bounds(0, 100, -60, 175))
                // build options
                .build();
        Observable<CellAndDistance> list = DistanceTravelledCalculator.getCellDistances(a, b,
                options);
        System.out.println("totalBearing=" + toPos(a).getBearingDegrees(toPos(b)));
        LongitudePair gcIntercept = toPos(a).getLongitudeOnGreatCircle(toPos(b), -31.0);
        System.out.println("gc long=" + gcIntercept);
        au.gov.amsa.util.navigation.Position nextPos = au.gov.amsa.util.navigation.Position
                .create(-31.0, gcIntercept.getLon2());
        System.out.println("bearingToFinal=" + nextPos.getBearingDegrees(toPos(b)));
        System.out.println("nextLat = " + nextPos.getLatitudeOnGreatCircle(toPos(b), 141.0));

        double totalNm = 0;
        for (CellAndDistance value : list.toList().toBlocking().single()) {
            System.out.println(value);
            totalNm += value.getDistanceNm();
        }
        assertEquals(toPos(a).getDistanceToKm(toPos(b)) / 1.852, totalNm, 0.2);
    }

    @Test
    public void testReplay() {
        ConnectableObservable<Integer> o = Observable.create(new OnSubscribe<Integer>() {

            volatile boolean firstTime = true;

            @Override
            public void call(Subscriber<? super Integer> sub) {
                if (firstTime) {
                    firstTime = false;
                    sub.onNext(1);
                }
                sub.onCompleted();
            }
        }).replay();
        o.connect();
        assertEquals(1, (int) o.count().toBlocking().single());
        assertEquals(1, (int) o.count().toBlocking().single());
    }

    @Test
    public void testConstantDifference() {
        List<Double> list = DistanceTravelledCalculator
                .makeConstantDifference(Arrays.asList(1.0, 2.0, 3.1, 4.2));
        assertEquals(4, list.size());
        double precision = 0.000001;
        assertEquals(1.0, list.get(0), precision);
        assertEquals(2.0, list.get(1), precision);
        assertEquals(3.0, list.get(2), precision);
        assertEquals(4.0, list.get(3), precision);
    }
}
