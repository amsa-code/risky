package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.DistanceTravelledCalculator.calculateTrafficDensity;
import static au.gov.amsa.geo.distance.Renderer.saveAsPng;
import static java.util.Optional.of;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.geo.Util;
import au.gov.amsa.geo.distance.DistanceTravelledCalculator.CalculationResult;
import au.gov.amsa.geo.model.Options;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

public class DistanceTravelledMovieMaker {

	private static Logger log = LoggerFactory
			.getLogger(DistanceTravelledMovieMaker.class);

	/**
	 * Saves a sequence of image files of Vessel Traffic Density plots to the
	 * <code>imageDirectory</code> with filenames map1.png, map2.png, etc.
	 * 
	 * @param options
	 * @param files
	 * @param times
	 * @param imageDirectory
	 */
	private static void saveImagesWithTimeRange(final Options options,
			final Observable<File> files, Observable<Long> times,
			final String imageDirectory) {

		times.buffer(2, 1).doOnNext(new Action1<List<Long>>() {
			AtomicInteger i = new AtomicInteger();

			@Override
			public void call(List<Long> pair) {
				if (pair.size() < 2)
					return;
				Long startTime = pair.get(0);
				Long finishTime = pair.get(1);
				saveImageWithTimeRange(options, files, startTime, finishTime,
						imageDirectory + "/map" + i.incrementAndGet() + ".png");
			}
		}).subscribe(reportErrors());

	}

	private static Observer<Object> reportErrors() {
		return new Observer<Object>() {

			@Override
			public void onCompleted() {
				log.info("completed");
			}

			@Override
			public void onError(Throwable e) {
				log.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}

			@Override
			public void onNext(Object t) {
				// do nothing
			}

		};
	}

	private static void saveImageWithTimeRange(Options options,
			final Observable<File> files, long startTime, long finishTime,
			String filename) {
		Options op = options.buildFrom()
		// set start time
				.startTime(of(startTime))
				// set finish time
				.finishTime(of(finishTime))
				// build
				.build();
		CalculationResult result = calculateTrafficDensity(op, files);
		saveAsPng(Renderer.createImage(op, 2, 1600, result), new File(filename));
	}

	private static Options createOptions(double cellSizeDegrees) {
		return Options.builder()
		// set origin latitude
				.originLat(0)
				// set origin longitudue
				.originLon(0)
				// square cell size in degrees
				.cellSizeDegrees(cellSizeDegrees)
				// set bounds
				// sabine bounds:
				// .bounds(new Bounds(-10, 110, -45, 158))
				// .bounds(new Bounds(15, 90, -20, 125))
				// build options
				.build();
	}

	public static void main(String[] args) {

		double cellSizeDegrees = 0.2;
		Options options = createOptions(cellSizeDegrees);

		String directory = System.getProperty("user.home")
				+ "/Downloads/positions-365-days";

		final Observable<File> files = Util.getFiles(directory, "craft-");

		Observable<Long> times = Observable.range(1, 13).map(
				new Func1<Integer, Long>() {

					@Override
					public Long call(Integer n) {
						return DateTime.parse("2013-06-01").plusMonths(n - 1)
								.getMillis();
					}
				});
		saveImagesWithTimeRange(options, files, times, "target");

	}

}
