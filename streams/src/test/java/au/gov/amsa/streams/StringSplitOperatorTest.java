package au.gov.amsa.streams;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;

public class StringSplitOperatorTest {

	@Test
	public void test() {
		Observable<String> o = Observable.just("boo:an", "d:you");
		List<String> expected = asList("boo", "and", "you");
		check(o, expected);
	}

	@Test
	public void testEmptyItemsEmitted() {
		Observable<String> o = Observable.just("::boo:an", "d:::you::");
		List<String> expected = asList("", "", "boo", "and", "", "", "you", "");
		check(o, expected);
	}

	@Test
	public void testEmptyItemsEmittedWithBackpressure() {
		Observable<String> o = Observable.just("::boo:an", "d:::you::");
		List<String> expected = asList("", "", "boo", "and", "", "", "you", "");
		checkWithBackpressure(o, expected);
	}

	@Test
	public void testWithBackpressure() throws InterruptedException {
		Observable<String> o = Observable.just("boo:an", "d:you");
		List<String> expected = asList("boo", "and", "you");
		checkWithBackpressure(o, expected);
	}

	private static void checkWithBackpressure(Observable<String> o,
			List<String> expected) {
		final List<String> list = new ArrayList<String>();
		o.lift(new StringSplitOperator(Pattern.compile(":"))).subscribe(
				createBackpressureSubscriber(list));
		assertEquals(expected, list);
	}

	private static void check(Observable<String> o, List<String> expected) {
		List<String> list = o
				.lift(new StringSplitOperator(Pattern.compile(":"))).toList()
				.toBlocking().single();
		assertEquals(expected, list);
	}

	private static Subscriber<String> createBackpressureSubscriber(
			final List<String> list) {
		final CountDownLatch latch = new CountDownLatch(1);
		return new Subscriber<String>() {

			@Override
			public void onStart() {
				request(1);
			}

			@Override
			public void onCompleted() {
				latch.countDown();
			}

			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			@Override
			public void onNext(String s) {
				list.add(s);
				request(1);
			}
		};
	}
}
