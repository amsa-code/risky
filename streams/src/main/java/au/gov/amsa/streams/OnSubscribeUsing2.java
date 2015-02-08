package au.gov.amsa.streams;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * Constructs an observable sequence that depends on a resource object.
 */
public final class OnSubscribeUsing2<T, Resource> implements OnSubscribe<T> {

	private final Func0<Resource> resourceFactory;
	private final Func1<? super Resource, ? extends Observable<? extends T>> observableFactory;
	private final Action1<? super Resource> dispose;
	private final boolean disposeEagerly;

	public OnSubscribeUsing2(
			Func0<Resource> resourceFactory,
			Func1<? super Resource, ? extends Observable<? extends T>> observableFactory,
			Action1<? super Resource> dispose, boolean disposeEagerly) {
		this.resourceFactory = resourceFactory;
		this.observableFactory = observableFactory;
		this.dispose = dispose;
		this.disposeEagerly = disposeEagerly;
	}

	@Override
	public void call(Subscriber<? super T> subscriber) {

		try {
			final Resource resource = resourceFactory.call();
			final Action0 disposeOnceOnly = createOnceOnlyDisposeAction(resource);
			subscriber.add(Subscriptions.create(disposeOnceOnly));
			Observable<? extends T> observable = observableFactory
			// create the observable
					.call(resource);
			if (disposeEagerly)
				observable = observable
				// dispose on completion or error
						.doOnTerminate(disposeOnceOnly);
			try {
				// start
				observable.unsafeSubscribe(subscriber);
			} catch (Throwable e) {
				Throwable ex;
				if (disposeEagerly)
					try {
						disposeOnceOnly.call();
						ex = e;
					} catch (Throwable e2) {
						// add the dispose error to the reported error
						ex = new CompositeException(Arrays.asList(e, e2));
					}
				else
					ex = e;
				// then propagate error
				subscriber.onError(ex);
			}
		} catch (Throwable e) {
			// then propagate error
			subscriber.onError(e);
		}
	}

	private Action0 createOnceOnlyDisposeAction(final Resource resource) {
		return new Action0() {

			final AtomicBoolean disposed = new AtomicBoolean(false);

			@Override
			public void call() {
				// only want dispose called once
				if (disposed.compareAndSet(false, true))
					dispose.call(resource);
			}
		};
	}
}
