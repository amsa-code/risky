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

public class BinaryFixesDriftDetectorMain {

	public static void main(String[] args) {
		// perform a speed test for loading BinaryFixes from disk

		// -downsample-5-mins
		List<File> files = Files.find(new File(
				"/media/an/binary-fixes/2014-year-downsample-5-mins"), Pattern
				.compile(".*\\.track"));
		int count = Observable
		// list files
				.from(files)
				// share the load between processors
				.buffer(Math.max(1, files.size()
						/ Runtime.getRuntime().availableProcessors()))
				// count each file asynchronously
				.flatMap(new Func1<List<File>, Observable<VesselPosition>>() {
					@Override
					public Observable<VesselPosition> call(List<File> list) {
						return Observable.from(list).concatMap(
								new Func1<File, Observable<VesselPosition>>() {

									@Override
									public Observable<VesselPosition> call(
											File file) {
										return new DriftingDetector()
												.getCandidates(BinaryFixes
														.from(file)
														.map(VesselPositions.TO_VESSEL_POSITION)
														.filter(new Func1<VesselPosition, Boolean>() {

															@Override
															public Boolean call(
																	VesselPosition vp) {
																return vp.speedMetresPerSecond().isPresent() && vp.speedMetresPerSecond().get() >0;
															}})
														//log
														.lift(Logging.<VesselPosition>logger().showValue().log()));

									}
								})
						// schedule
						 .subscribeOn(Schedulers.computation())
						;
					}
				})
				.count()
				.toBlocking().single();
		System.out.println("drift detections = "+ count);
	}

}
