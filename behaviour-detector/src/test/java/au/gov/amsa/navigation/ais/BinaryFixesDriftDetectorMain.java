package au.gov.amsa.navigation.ais;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.navigation.DriftingDetector;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPositions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.slf4j.Logging;
import com.github.davidmoten.rx.slf4j.Logging.Level;
import com.github.davidmoten.rx.slf4j.OperatorLogging;

public class BinaryFixesDriftDetectorMain {

	public static void main(String[] args) {

		List<File> files = Files.find(new File("/media/an/binary-fixes-2012"),
		        Pattern.compile(".*\\.track"));
		final OperatorLogging<VesselPosition> logger = Logging.<VesselPosition> logger()
		        .onCompleted(Level.TRACE).showCount().every(1000000).log();
		int count = Observable
		// list files
		        .from(files)
		        // share the load between processors
		        .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors()))
		        // count each file asynchronously
		        .flatMap(new Func1<List<File>, Observable<VesselPosition>>() {
			        @Override
			        public Observable<VesselPosition> call(List<File> list) {
				        return Observable.from(list)
				                .concatMap(new Func1<File, Observable<VesselPosition>>() {

					                @Override
					                public Observable<VesselPosition> call(File file) {
						                return BinaryFixes.from(file)
						                        .map(VesselPositions.TO_VESSEL_POSITION)
						                        .lift(logger)
						                        .compose(DriftingDetector.detectDrift())
						                        .onBackpressureBuffer();
					                }
				                })
				                // schedule
				                .subscribeOn(Schedulers.computation());
			        }
		        }).lift(Logging.<VesselPosition> logger().showCount().every(100000).log())
		        // count
		        .count().toBlocking().single();
		System.out.println("drift detections = " + count);
	}
}
