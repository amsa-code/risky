package au.gov.amsa.ais.rx;

import java.util.HashMap;
import java.util.Map;

import rx.functions.Func1;

public class Cache<S, T> {

	private final Func1<S, T> factory;

	public Cache(Func1<S, T> factory) {
		this.factory = factory;
	}

	private final Map<S, T> map = new HashMap<S, T>();

	public synchronized Cache<S, T> put(S key, T value) {
		map.put(key, value);
		return this;
	}

	public synchronized T get(S key) {
		T value = map.get(key);
		if (value == null) {
			T t = factory.call(key);
			map.put(key, t);
			return t;
		} else
			return value;
	}
}
