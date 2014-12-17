package au.gov.amsa.streams;

import java.io.IOException;
import java.io.Reader;

import rx.Subscriber;
import rx.observables.AbstractOnSubscribe;

public class ReaderOnSubscribe extends AbstractOnSubscribe<String, Reader> {

	private final int size;
	private final Reader reader;

	public ReaderOnSubscribe(int size, Reader reader) {
		this.size = size;
		this.reader = reader;
	}

	@Override
	protected Reader onSubscribe(Subscriber<? super String> subscriber) {
		return reader;
	}

	@Override
	protected void next(
			rx.observables.AbstractOnSubscribe.SubscriptionState<String, Reader> state) {

		Reader reader = state.state();
		char[] buffer = new char[size];
		try {
			int count = reader.read(buffer);
			if (count == -1)
				state.onCompleted();
			else
				state.onNext(String.valueOf(buffer, 0, count));
		} catch (IOException e) {
			state.onError(e);
		}
	}
}
