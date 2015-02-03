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
		final String inputFilename;
		if (args.length > 0)
			inputFilename = args[0];
		else
			// inputFilename = "G:\\mariweb";
			inputFilename = "/media/analysis/test";

		File input = new File(inputFilename);
		File output = new File("target/binary");
		long t = System.currentTimeMillis();

		int logEvery = 100000;
		// String pattern = "NMEA_ITU_(201412|20150101).*.gz";
		String pattern = "NMEA.*.gz";
		Pattern inputPattern = Pattern.compile(pattern);

		if (false) {

			BinaryFixesWriter.writeFixes(input, inputPattern, output, logEvery)
					.observeOn(Schedulers.immediate())
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
}
