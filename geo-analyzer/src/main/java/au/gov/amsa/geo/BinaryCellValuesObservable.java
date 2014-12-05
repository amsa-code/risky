package au.gov.amsa.geo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import au.gov.amsa.geo.model.CellValue;

public class BinaryCellValuesObservable {

	/**
	 * Returns a sequence one Double value being the cellSizeDegrees followed by
	 * multiple {@link CellValueImpl}s.
	 * 
	 * @param is
	 * @return
	 */
	public static Observable<?> readValues(final InputStream is) {
		return Observable.create(createOnSubscribe(null, is));
	}

	public static Observable<?> readValues(final File file) {
		return Observable.create(createOnSubscribe(file, null));
	}

	private static OnSubscribe<Object> createOnSubscribe(final File file,
			final InputStream is) {
		return new OnSubscribe<Object>() {
			@Override
			public void call(Subscriber<Object> subscriber) {
				final InputStream inputStream;
				if (file != null) {
					try {
						inputStream = new FileInputStream(file);
					} catch (FileNotFoundException e) {
						subscriber.onError(e);
						return;
					}
				} else
					inputStream = is;
				DataInputStream in = new DataInputStream(
						new BufferedInputStream(inputStream, 8192));
				try {
					double cellSizeDegrees = in.readDouble();
					subscriber.onNext(cellSizeDegrees);
					// topLeftLat
					in.readDouble();
					// topLeftLon
					in.readDouble();
					// bottomRightLat
					in.readDouble();
					// bottomRightLon
					in.readDouble();
					while (!subscriber.isUnsubscribed()) {
						double centreLat = in.readFloat();
						double centreLon = in.readFloat();
						double value = in.readDouble();
						subscriber.onNext(new CellValue(centreLat, centreLon,
								value));
					}
					close(in);
					subscriber.onCompleted();
				} catch (EOFException e) {
					close(in);
					subscriber.onCompleted();
				} catch (Throwable e) {
					close(in);
					subscriber.onError(e);
				}
			}

			private void close(DataInputStream in) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore close problem
				}
			}
		};

	}

}
