package au.gov.amsa.streams;

import rx.Observable;

public class Ob {

	// TODO move this class to rxjava-extras

	public static <T> Observable<T> justOne(T t) {
		return Observable.create(new OnSubscribeJustOneWithBackpressure<T>(t));
	}

}
