package au.gov.amsa.risky.format;

import static au.gov.amsa.risky.format.BinaryFixes.BINARY_FIX_BYTES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;

public class BinaryFixesOnSubscribe implements OnSubscribe<Fix> {

	private static final Logger log = LoggerFactory.getLogger(BinaryFixesOnSubscribe.class);

	private File file;

	public BinaryFixesOnSubscribe(File file) {
		this.file = file;
	}

	public static Observable<Fix> from(File file) {
		return Observable.create(new BinaryFixesOnSubscribe(file));
	}

	@Override
	public void call(Subscriber<? super Fix> subscriber) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			subscriber.add(createSubscription(fis));
			reportFixes(BinaryFixesUtil.getMmsi(file), subscriber, fis);
			if (!subscriber.isUnsubscribed())
				subscriber.onCompleted();
		} catch (Exception e) {
			if (!subscriber.isUnsubscribed())
				subscriber.onError(e);
		}
	}

	private Subscription createSubscription(final FileInputStream fis) {
		return new Subscription() {

			volatile boolean subscribed = true;

			@Override
			public void unsubscribe() {
				subscribed = false;
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean isUnsubscribed() {
				return !subscribed;
			}
		};
	}

	private static void reportFixes(long mmsi, Subscriber<? super Fix> subscriber, InputStream fis)
	        throws IOException {
		byte[] bytes = new byte[4096 * BINARY_FIX_BYTES];
		int length = 0;
		if (subscriber.isUnsubscribed())
			return;
		while ((length = fis.read(bytes)) > 0) {
			for (int i = 0; i < length; i += BINARY_FIX_BYTES) {
				if (subscriber.isUnsubscribed())
					return;
				ByteBuffer bb = ByteBuffer.wrap(bytes, i, BINARY_FIX_BYTES);
				try {
					Fix fix = BinaryFixesUtil.toFix(mmsi, bb);
					subscriber.onNext(fix);
				} catch (RuntimeException e) {
					log.warn(e.getMessage());
				}
			}
		}
	}

}
