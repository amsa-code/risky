package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.Util.toPos;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.assertEquals;
import static rx.Observable.from;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.geo.model.Position;
import au.gov.amsa.util.navigation.Position.LongitudePair;

public class DistanceTravelledCalculatorTest {

	private static final Fix f1 = new Fix("f1", -35, 142.0, 0);
	private static final Fix f3 = new Fix("f3", -35.12, 142.12,
			HOURS.toMillis(2));
	private static final Fix f4 = new Fix("f4", -36.12, 143.12,
			HOURS.toMillis(2));

	@Test
	public void testPairs() {
		System.out.println(from(asList(1, 2, 3)).buffer(2, 1).toList().toBlocking()
				.single());
	}

	@Test
	public void testWithFiles() {

		long t = System.currentTimeMillis();
		Observable<File> files = from(new File("src/test/resources/positions")
				.listFiles(new FileFilter() {

					@Override
					public boolean accept(File file) {
						return file.getName().startsWith("craft-");
					}
				}));
		final AtomicInteger count = new AtomicInteger();
		DistanceCalculationMetrics metrics = new DistanceCalculationMetrics();
		new DistanceTravelledCalculator(Options.builder().originLat(0)
				.originLon(0).cellSizeDegrees(0.1)
				.bounds(new Bounds(0, 120, -60, 175)).build(), metrics)
				.calculateDistanceByCellFromFiles(files).toBlocking()
				.forEach(new Action1<CellAndDistance>() {

					@Override
					public void call(CellAndDistance cell) {
						System.out.println(cell);
						count.incrementAndGet();
					}
				});
		System.out.println(count.get() + " cells returned in "
				+ (System.currentTimeMillis() - t) + "ms");
	}

	@Test
	public void testGetCellDistances() {

		Options options = Options.builder().originLat(0).originLon(0)
				.cellSizeDegrees(0.1).bounds(new Bounds(0, 100, -60, 175))
				.build();
		Observable<CellAndDistance> list = DistanceTravelledCalculator
				.getCellDistances(f1, f3, options);
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
		Observable<CellAndDistance> list = DistanceTravelledCalculator
				.getCellDistances(f1, f4, options);
		System.out.println("totalBearing="
				+ toPos(f1).getBearingDegrees(toPos(f4)));

		double totalNm = 0;
		for (CellAndDistance value : list.toList().toBlocking().single()) {
			System.out.println(value);
			totalNm += value.getDistanceNm();
		}
		assertEquals(toPos(f1).getDistanceToKm(toPos(f4)) / 1.852, totalNm,
				0.00001);
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
		Observable<CellAndDistance> list = DistanceTravelledCalculator
				.getCellDistances(a, b, options);
		System.out.println("totalBearing="
				+ toPos(a).getBearingDegrees(toPos(b)));
		LongitudePair gcIntercept = toPos(a).getLongitudeOnGreatCircle(
				toPos(b), -31.0);
		System.out.println("gc long=" + gcIntercept);
		au.gov.amsa.util.navigation.Position nextPos = au.gov.amsa.util.navigation.Position
				.create(-31.0, gcIntercept.getLon2());
		System.out.println("bearingToFinal="
				+ nextPos.getBearingDegrees(toPos(b)));
		System.out.println("nextLat = "
				+ nextPos.getLatitudeOnGreatCircle(toPos(b), 141.0));

		double totalNm = 0;
		for (CellAndDistance value : list.toList().toBlocking().single()) {
			System.out.println(value);
			totalNm += value.getDistanceNm();
		}
		assertEquals(toPos(a).getDistanceToKm(toPos(b)) / 1.852, totalNm, 0.2);
	}

	@Test
	public void testReplay() {
		ConnectableObservable<Integer> o = Observable.create(
				new OnSubscribe<Integer>() {

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
}
