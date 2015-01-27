package au.gov.amsa.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import rx.Subscriber;
import rx.observables.AbstractOnSubscribe;

public class InputStreamOnSubscribe extends
		AbstractOnSubscribe<byte[], InputStream> {

	private final InputStream reader;
	private final int size;

	public InputStreamOnSubscribe(InputStream reader, int size) {
		this.reader = reader;
		this.size = size;
	}

	@Override
	protected InputStream onSubscribe(Subscriber<? super byte[]> subscriber) {
		return reader;
	}

	@Override
	protected void next(
			rx.observables.AbstractOnSubscribe.SubscriptionState<byte[], InputStream> state) {

		InputStream reader = state.state();
		byte[] buffer = new byte[size];
		try {
			int count = reader.read(buffer);
			if (count == -1)
				state.onCompleted();
			else
				state.onNext(Arrays.copyOf(buffer, count));
		} catch (IOException e) {
			state.onError(e);
		}
	}
}
