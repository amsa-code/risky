package au.gov.amsa.streams;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.operators.OnSubscribeUsing;

public class Ob {

	public static <T> Observable<T> justOne(T t) {
		return Observable.create(new OnSubscribeJustOneWithBackpressure<T>(t));
	}

	public final static <T, Resource> Observable<T> using(
			final Func0<Resource> resourceFactory,
			final Func1<? super Resource, ? extends Observable<? extends T>> observableFactory,
			final Action1<? super Resource> disposeAction) {
		return Observable.create(new OnSubscribeUsing<T, Resource>(
				resourceFactory, observableFactory, disposeAction));
	}

}
