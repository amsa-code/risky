package au.gov.amsa.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;
import au.gov.amsa.geo.model.Fix;

/**
 * Returns a sequence of {@link Fix}es from a CTS extract encoded as a binary
 * file by {@link CtsAnalyzer}.
 */
public class BinaryFixesObservable {

	private static final int LOG_EVERY = 1000000;
	private static int NUM_BYTES_PER_REPORT = 4 + 4 + 8;
	private static final Logger log = Logger
			.getLogger(BinaryFixesObservable.class);

	public static final Func1<File, Observable<Fix>> TO_FIXES = new Func1<File, Observable<Fix>>() {
		AtomicLong count = new AtomicLong(0);

		@Override
		public Observable<Fix> call(final File file) {
			return observeFile(file, count);
		}
	};

	private static Observable<Fix> observeFile(final File file,
			final AtomicLong fixesCount) {
		return Observable.create(new OnSubscribe<Fix>() {

			@Override
			public void call(Subscriber<? super Fix> subscriber) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					reportFixes(file, fixesCount, subscriber, fis);
					subscriber.onCompleted();
				} catch (Exception e) {
					subscriber.onError(e);
				} finally {
					closeQuietly(fis);
				}
			}

		});
	}

	private static void reportFixes(final File file,
			final AtomicLong fixesCount, Subscriber<? super Fix> subscriber,
			InputStream fis) throws IOException {
		byte[] bytes = new byte[4096 * NUM_BYTES_PER_REPORT];
		int length = 0;
		if (subscriber.isUnsubscribed())
			return;
		String craftId = getCraftId(file);
		while ((length = fis.read(bytes)) > 0) {
			for (int i = 0; i < length; i += NUM_BYTES_PER_REPORT) {
				if (subscriber.isUnsubscribed())
					return;
				ByteBuffer bb = ByteBuffer.wrap(bytes, i, NUM_BYTES_PER_REPORT);
				float lat = bb.getFloat();
				float lon = bb.getFloat();
				long time = bb.getLong();
				Fix fix = new Fix(craftId, lat, lon, time);
				long count = fixesCount.incrementAndGet();
				if (count % LOG_EVERY == 0)
					log.info("fixes = " + (count / LOG_EVERY) + "million, "
							+ Util.memoryUsage());
				subscriber.onNext(fix);
			}
		}
	}

	private static void closeQuietly(FileInputStream fis) {
		if (fis != null)
			try {
				fis.close();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
	}

	public static String getCraftId(File file) {
		int start = file.getName().indexOf("-");
		int finish = file.getName().length();
		String craftId = file.getName().substring(start + 1, finish);
		return craftId;
	}

}
