package au.gov.amsa.streams;

import java.io.Reader;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;

/**
 * Utilities for stream processing of lines of text from
 * <ul>
 * <li>sockets</li>
 * </ul>
 */
public final class Strings {

	/**
	 * Returns null if input is null otherwise returns input.toString().trim().
	 */
	public static Func1<Object, String> TRIM = new Func1<Object, String>() {

		@Override
		public String call(Object input) {
			if (input == null)
				return null;
			else
				return input.toString().trim();
		}
	};

	// public static Observable<byte[]> from(final InputStream is, final int
	// size) {
	// return Observable.create(new OnSubscribe<byte[]>() {
	// @Override
	// public void call(Subscriber<? super byte[]> subscriber) {
	// subscriber.setProducer(new InputStreamProducer(is, subscriber,
	// size));
	// }
	// });
	// }
	//
	// public static Observable<byte[]> from(InputStream is) {
	// return from(is, 8192);
	// }

	public static Observable<String> from(final Reader reader, final int size) {
		return new ReaderOnSubscribe(reader, size).toObservable();
		// return StringObservable.from(reader, size);
	}

	public static Observable<String> from(Reader reader) {
		return from(reader, 8192);
	}

	public static Observable<String> split(Observable<String> source,
			String pattern) {
		return source.lift(new StringSplitOperator(Pattern.compile(pattern)));
	}

}
