package au.gov.amsa.streams;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscriber;

class ReaderProducer implements Producer {

	private final AtomicLong requested = new AtomicLong(0);
	private final Reader reader;
	private final Subscriber<? super String> subscriber;
	private final int size;

	ReaderProducer(Reader reader, Subscriber<? super String> subscriber,
			int size) {
		this.reader = reader;
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
		char[] buffer = new char[size];
		try {
			if (subscriber.isUnsubscribed())
				return;
			int n = reader.read(buffer);
			while (n != -1 && !subscriber.isUnsubscribed()) {
				subscriber.onNext(new String(Arrays.copyOf(buffer, n)));
				if (!subscriber.isUnsubscribed())
					n = reader.read(buffer);
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
			char[] buffer = new char[size];
			while (true) {
				long r = requested.get();
				long numToEmit = r;

				// emit numToEmit

				if (subscriber.isUnsubscribed())
					return;
				int numRead;
				if (numToEmit > 0)
					numRead = reader.read(buffer);
				else
					numRead = 0;
				while (numRead != -1 && !subscriber.isUnsubscribed()
						&& numToEmit > 0) {
					subscriber
							.onNext(new String(Arrays.copyOf(buffer, numRead)));
					numToEmit--;
					if (numToEmit > 0 && !subscriber.isUnsubscribed())
						numRead = reader.read(buffer);
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