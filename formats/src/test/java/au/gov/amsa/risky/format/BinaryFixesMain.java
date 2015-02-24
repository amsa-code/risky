package au.gov.amsa.risky.format;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.slf4j.Logging;

public class BinaryFixesMain {

	public static void main(String[] args) {
		//perform a speed test for loading BinaryFixes from disk
		
		final ConcurrentHashMap<Long, List<Fix>> map = new ConcurrentHashMap<Long, List<Fix>>();
		
		List<File> files = Files.find(new File(
				"/media/an/binary-fixes/2014-year-downsample-5-mins"), Pattern
				.compile(".*\\.track"));
		long t = System.currentTimeMillis();
		long count = Observable
		// list files
				.from(files)
				// count each file asynchronously
				.flatMap(new Func1<File, Observable<Long>>() {
					@Override
					public Observable<Long> call(File file) {
						return BinaryFixes.from(file)
								//add to map
								//.doOnNext(addToMap(map))
								//count
								.countLong()
								.subscribeOn(Schedulers.computation());
					}
					
				})
				// total counts
				.scan(0L, new Func2<Long, Long, Long>() {
					@Override
					public Long call(Long a, Long b) {
						return a + b;
					}
				})
				// log count so far
				.lift(Logging.<Long> logger().showCount().prefix("records=")
						.showMemory().every(1000).log())
				// get last count (total)
				.last()
				//block and get
				.toBlocking().single();
		long elapsed = System.currentTimeMillis() - t;
		System.out.println("Map size = " + map.size());
		System.out.println("Total records = " + count + ", numPerSecond="
				+ count * 1000.0 / elapsed + ", timeMs=" + elapsed);
	}
	
	private static Action1<? super Fix> addToMap(final ConcurrentHashMap<Long, List<Fix>> map){
		return new Action1<Fix>() {
			@Override
			public void call(Fix fix) {
					List<Fix> list = map.get(fix.getMmsi());
					if (list==null) {
						map.putIfAbsent(fix.getMmsi(), Collections.synchronizedList( new ArrayList<Fix>()));
						list = map.get(fix.getMmsi());
					}
					list.add(fix);
			}};
	}
}
