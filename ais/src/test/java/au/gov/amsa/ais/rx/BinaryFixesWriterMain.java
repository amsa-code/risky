package au.gov.amsa.ais.rx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.Timestamped;

import com.github.davidmoten.rx.slf4j.Logging;

public class BinaryFixesWriterMain {

	private static final Logger log = LoggerFactory
			.getLogger(BinaryFixesWriterMain.class);

	public static void main(String[] args) throws IOException {
		log.info("starting");

		System.getProperty("rx.ring-buffer.size", "8192");

		final String inputFilename = prop("input", "/media/analysis/test");
		final String outputFilename = prop("output", "target/binary");
		final String pattern = prop("pattern", "NMEA_ITU_.*.gz");

		log.info("Converting NMEA files in " + inputFilename);
		log.info("to BinaryFixes format in " + outputFilename);
		log.info("using pattern='" + pattern + "'");

		File input = new File(inputFilename);
		File output = new File(outputFilename);
		long t = System.currentTimeMillis();

		int logEvery = 100000;
		int writeBufferSize = 1000;
		Pattern inputPattern = Pattern.compile(pattern);

		if (true) {

			BinaryFixesWriter
					.writeFixes(input, inputPattern, output, logEvery,
							writeBufferSize).observeOn(Schedulers.immediate())
					.subscribe(new Observer<Integer>() {

						@Override
						public void onCompleted() {

						}

						@Override
						public void onError(Throwable e) {
							e.printStackTrace();
						}

						@Override
						public void onNext(Integer t) {
							// TODO Auto-generated method stub

						}
					});
		} else {
			// read 11 million NMEA lines
			Streams.nmeaFromGzip("/media/analysis/test/NMEA_ITU_20150101.gz")
			// buffer in groups of 20,000 to assign to computation
			// threads
					.buffer(20000)
					// parse the messages asynchronously using computation
					// scheduler
					.flatMap(
							new Func1<List<String>, Observable<Timestamped<AisMessage>>>() {
								@Override
								public Observable<Timestamped<AisMessage>> call(
										List<String> list) {
									return Streams.extractMessages(
											Observable.from(list))
									// do async
											.subscribeOn(
													Schedulers.computation());
								}
							})
					// log stuff
					.lift(Logging.<Timestamped<AisMessage>> logger()
							.showRateSince("rate=", 1000).showCount()
							.every(100000).log())
					// count emitted
					.count()
					// observer results in current thread
					.observeOn(Schedulers.immediate())
					// go
					.subscribe(new Observer<Integer>() {

						@Override
						public void onCompleted() {

						}

						@Override
						public void onError(Throwable e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}

						@Override
						public void onNext(Integer n) {
							System.out.println(n + " items");
						}
					});
		}

		log.info("finished in " + (System.currentTimeMillis() - t) / 1000.0
				+ "s");
	}

	private static String prop(String name, String defaultValue) {
		return System.getProperty(name, defaultValue);
	}
}
