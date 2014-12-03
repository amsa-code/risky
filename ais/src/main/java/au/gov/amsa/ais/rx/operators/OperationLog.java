package au.gov.amsa.ais.rx.operators;

import java.text.SimpleDateFormat;
import java.util.Date;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

public class OperationLog {

	public static <T> OnSubscribeFunc<T> log(Observable<? extends T> that) {
		return new Log<T>(that);
	}

	private static class Log<T> implements OnSubscribeFunc<T> {

		private final Observable<? extends T> source;

		Log(Observable<? extends T> source) {
			this.source = source;
		}

		@Override
		public Subscription onSubscribe(final Observer<? super T> observer) {
			final Subscription sub = source.subscribe(new Observer<T>() {

				@Override
				public void onCompleted() {
					log("calling onCompleted");
					observer.onCompleted();
					log("called onCompleted");
				}

				@Override
				public void onError(Throwable e) {
					log("calling onError");
					observer.onError(e);
					log("called onError");
				}

				@Override
				public void onNext(T args) {
					log("calling onNext " + args);
					observer.onNext(args);
					log("called onNext");
				}
			});
			return new Subscription() {
				@Override
				public void unsubscribe() {
					log("unsubscribing");
					sub.unsubscribe();
					log("unsubscribed");
				}

				@Override
				public boolean isUnsubscribed() {
					return sub.isUnsubscribed();
				}
			};
		}
	}

	private static void log(String message) {
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
				.format(new Date())
				+ " - "
				+ Thread.currentThread().getName()
				+ " - " + message);
	}
}
