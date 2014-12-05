package au.gov.amsa.util.rx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeException extends Exception {

	private static final long serialVersionUID = -1150240783814840391L;

	private final List<Throwable> exceptions;

	public CompositeException(Throwable... exceptions) {
		this.exceptions = Arrays.asList(exceptions);
	}

	public List<Throwable> getExceptions() {
		return new ArrayList<Throwable>(exceptions);
	}
}