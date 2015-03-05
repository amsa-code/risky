package au.gov.amsa.streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class Bytes {

	public static Observable<byte[]> from(final InputStream is, final int size) {
		return new InputStreamOnSubscribe(is, size).toObservable();
	}

	public static Observable<byte[]> from(InputStream is) {
		return from(is, 8192);
	}

	public static Observable<byte[]> from(final File file) {
		Func0<InputStream> resourceFactory = new Func0<InputStream>() {
			@Override
			public InputStream call() {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
		Func1<InputStream, Observable<byte[]>> observableFactory = new Func1<InputStream, Observable<byte[]>>() {
			@Override
			public Observable<byte[]> call(InputStream is) {
				return from(is);
			}
		};
		Action1<InputStream> disposeAction = new Action1<InputStream>() {
			@Override
			public void call(InputStream is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		return Observable.using(resourceFactory, observableFactory,
				disposeAction, true);
	}
}
