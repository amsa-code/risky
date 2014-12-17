import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class BackpressureGcMain {

	public static void main(String... args) throws InterruptedException {
		Observable
				.range(0, 20)
				// batch the filenames so we don't overload merge
				.buffer(Runtime.getRuntime().availableProcessors() - 1)
				.map(new Func1<List<Integer>, Observable<Integer>>() {
					@Override
					public Observable<Integer> call(List<Integer> list) {
						return Observable.from(list);
					}
				})
				.concatMap(
						new Func1<Observable<Integer>, Observable<Integer>>() {
							@Override
							public Observable<Integer> call(
									Observable<Integer> n) {
								return Observable
								// integers for ever
										.range(0, Integer.MAX_VALUE)
										// use up some heap
										.flatMap(
												new Func1<Integer, Observable<Integer>>() {
													@Override
													public Observable<Integer> call(
															Integer m) {
														return Observable
																.from(Collections
																		.singleton(m));
													}
												})
										// async
										.subscribeOn(Schedulers.computation());
							}
						}).subscribe();
		Thread.sleep(100000000L);
	}

}
