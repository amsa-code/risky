package au.gov.amsa.risky.format;

import rx.Observable;
import rx.Observable.Transformer;

public final class Transformers {

	public static <T> Transformer<T, T> identity() {
		return new Transformer<T, T>() {

			@Override
			public Observable<T> call(Observable<T> o) {
				return o;
			}
		};
	}

}
