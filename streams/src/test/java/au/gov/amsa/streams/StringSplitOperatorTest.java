package au.gov.amsa.streams;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;

public class StringSplitOperatorTest {

	@Test
	public void test() {
		List<String> list = Observable.just("boo:an", "d:you")
				.lift(new StringSplitOperator(Pattern.compile(":"))).toList()
				.toBlocking().single();
		assertEquals(asList("boo", "and", "you"), list);
	}

	@Test
	public void testWithBackpressure() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final List<String> list = new ArrayList<String>();
		Observable.just("boo:an", "d:you")
				.lift(new StringSplitOperator(Pattern.compile(":")))
				.subscribe(new Subscriber<String>() {

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
				});
		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertEquals(asList("boo", "and", "you"), list);
	}
}
