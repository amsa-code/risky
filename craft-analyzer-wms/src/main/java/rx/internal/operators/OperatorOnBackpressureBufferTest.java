package rx.internal.operators;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.RxRingBuffer;
import rx.schedulers.Schedulers;

public class OperatorOnBackpressureBufferTest {

	@Test
	public void testMergeKeepsRequesting() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observable.range(1, 2)
		// produce many integers per second
				.flatMap(new Func1<Integer, Observable<Integer>>() {
					@Override
					public Observable<Integer> call(final Integer number) {
						return Observable.range(1, Integer.MAX_VALUE)
						// pause a bit
								.doOnNext(pauseForMs(1))
								// buffer on backpressure
								.onBackpressureBuffer()
								// do in parallel
								.subscribeOn(Schedulers.computation());
					}

				})
				// take a number bigger than 2* RxRingBuffer.SIZE (used by
				// OperatorMerge)
				.take(RxRingBuffer.SIZE * 2 + 1)
				// log count
				.doOnNext(printCount())
				// release latch
				.doOnCompleted(new Action0() {
					@Override
					public void call() {
						latch.countDown();
					}
				}).subscribe();
		assertTrue(latch.await(1, TimeUnit.SECONDS));
	}

	private static Action1<Integer> printCount() {
		return new Action1<Integer>() {
			long count;

			@Override
			public void call(Integer t1) {
				count++;
				System.out.println("count=" + count);
			}
		};
	}

	private static Action1<Integer> pauseForMs(final long time) {
		return new Action1<Integer>() {
			@Override
			public void call(Integer s) {
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@Test
	public void test2() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Observable.range(1, Integer.MAX_VALUE)
				.delay(1, TimeUnit.MILLISECONDS, Schedulers.immediate())
				.subscribeOn(Schedulers.computation())
				.mergeWith(Observable.<Integer> empty()).take(300)
				.doOnNext(new Action1<Integer>() {
					long count;

					@Override
					public void call(Integer n) {
						count++;
						System.out.println("outside=" + count);
					}
				})// release latch
				.doOnCompleted(new Action0() {
					@Override
					public void call() {
						latch.countDown();
					}
				}).subscribe();
		assertTrue(latch.await(5, TimeUnit.SECONDS));
	}

}
