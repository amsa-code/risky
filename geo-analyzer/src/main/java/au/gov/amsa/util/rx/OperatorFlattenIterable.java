package au.gov.amsa.util.rx;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;

public class OperatorFlattenIterable<T> implements Operator<T, Iterable<T>> {

	public static <T> Operator<T, Iterable<T>> flatten() {
		return new OperatorFlattenIterable<T>();
	}

	@Override
	public Subscriber<? super Iterable<T>> call(
			final Subscriber<? super T> subscriber) {
		return new Subscriber<Iterable<T>>(subscriber) {

			@Override
			public void onCompleted() {
				subscriber.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				subscriber.onError(e);
			}

			@Override
			public void onNext(Iterable<T> list) {
				for (T t : list) {
					if (subscriber.isUnsubscribed()) {
						return;
					}
					try {
						subscriber.onNext(t);
					} catch (Exception e) {
						onError(OnErrorThrowable.addValueAsLastCause(e, t));
						return;
					}
				}
			}
		};
	}
}