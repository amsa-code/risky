package rx.functions;

public class Functions {

	public static <T> Func1<T, T> identity() {
		return new Func1<T, T>() {
			@Override
			public T call(T t) {
				return t;
			}
		};
	}

}
