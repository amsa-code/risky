package au.gov.amsa.streams;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;

public class OnSubscribeJustOneWithBackpressure<T> implements OnSubscribe<T> {

	private final T value;

	public OnSubscribeJustOneWithBackpressure(T t) {
		value = t;
	}

	@Override
	public void call(final Subscriber<? super T> child) {

		child.setProducer(new Producer() {

			private final AtomicBoolean emitted = new AtomicBoolean(false);

			@Override
			public void request(long n) {
				if (n > 0) {
					if (emitted.compareAndSet(false, true)) {
						child.onNext(value);
						child.onCompleted();
					}
				}
			}
		});
	}

}
