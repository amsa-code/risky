package au.gov.amsa.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscriber;

class InputStreamProducer implements Producer {

	private final AtomicLong requested = new AtomicLong(0);
	private final InputStream is;
	private final Subscriber<? super byte[]> subscriber;
	private final int size;

	InputStreamProducer(InputStream is, Subscriber<? super byte[]> subscriber,
			int size) {
		this.is = is;
		this.subscriber = subscriber;
		this.size = size;
	}

	@Override
	public void request(long n) {
		try {
			if (requested.get() == Long.MAX_VALUE)
				// already started with fast path
				return;
			else if (n == Long.MAX_VALUE) {
				// fast path
				requestAll();
			} else
				requestSome(n);
		} catch (RuntimeException e) {
			subscriber.onError(e);
		} catch (IOException e) {
			subscriber.onError(e);
		}
	}

	private void requestAll() {
		requested.set(Long.MAX_VALUE);
		byte[] buffer = new byte[size];
		try {
			if (subscriber.isUnsubscribed())
				return;
			int n = is.read(buffer);
			while (n != -1 && !subscriber.isUnsubscribed()) {
				subscriber.onNext(Arrays.copyOf(buffer, n));
				if (!subscriber.isUnsubscribed())
					n = is.read(buffer);
			}
		} catch (IOException e) {
			subscriber.onError(e);
		}
		if (subscriber.isUnsubscribed())
			return;
		subscriber.onCompleted();
	}

	private void requestSome(long n) throws IOException {
		// back pressure path
		// this algorithm copied roughly from
		// rxjava/OnSubscribeFromIterable.java

		long previousCount = requested.getAndAdd(n);
		if (previousCount == 0) {
			byte[] buffer = new byte[size];
			while (true) {
				long r = requested.get();
				long numToEmit = r;

				// emit numToEmit

				if (subscriber.isUnsubscribed())
					return;
				int numRead;
				if (numToEmit > 0)
					numRead = is.read(buffer);
				else
					numRead = 0;
				while (numRead != -1 && !subscriber.isUnsubscribed()
						&& numToEmit > 0) {
					subscriber.onNext(Arrays.copyOf(buffer, numRead));
					numToEmit--;
					if (numToEmit > 0 && !subscriber.isUnsubscribed())
						numRead = is.read(buffer);
				}

				// check if we have finished
				if (numRead == -1 && !subscriber.isUnsubscribed())
					subscriber.onCompleted();
				else if (subscriber.isUnsubscribed())
					return;
				else if (requested.addAndGet(-r) == 0)
					return;
			}
		}
	}
}