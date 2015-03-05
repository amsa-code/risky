package au.gov.amsa.navigation.ais;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.navigation.DriftingDetector;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPositions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;

public class BinaryFixesDriftDetectorMain {

	private static final Logger log = LoggerFactory.getLogger(BinaryFixesDriftDetectorMain.class);

	public static void main(String[] args) {

		List<File> files = Files.find(new File("/media/an/binary-fixes-2014-5-minutes"),
		        Pattern.compile(".*\\.track"));
		log.info("files=" + files.size());
		final AtomicLong num = new AtomicLong();
		int count = Observable
		// list files
		        .from(files)
		        // share the load between processors
		        .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
		        // search each list of files for drift detections
		        .flatMap(detectDrift(num))
		        // log
		        // .lift(Logging.<VesselPosition>
		        // logger().showCount().showValue().every(10000).log())
		        // count
		        .reduce(0, new Func2<Integer, Integer, Integer>() {
			        @Override
			        public Integer call(Integer a, Integer b) {
				        return a + b;
			        }
		        }).toBlocking().single();
		System.out.println("drift detections = " + count);
	}

	private static Func1<List<File>, Observable<Integer>> detectDrift(final AtomicLong num) {
		return new Func1<List<File>, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(List<File> list) {
				return Observable.from(list).concatMap(new Func1<File, Observable<Integer>>() {
					@Override
					public Observable<Integer> call(File file) {
						return BinaryFixes.from(file).map(VesselPositions.TO_VESSEL_POSITION)
						        .doOnNext(logCount(num)).compose(DriftingDetector.detectDrift())
						        .count();
					}
				})
				// schedule
				        .subscribeOn(Schedulers.computation());
			}
		};
	}

	private static Action1<VesselPosition> logCount(final AtomicLong num) {
		return new Action1<VesselPosition>() {
			@Override
			public void call(VesselPosition p) {
				long n = num.incrementAndGet();
				if (n % 1000000 == 0)
					log.info((n / 1000000.0) + "m");
			}
		};
	}

}
