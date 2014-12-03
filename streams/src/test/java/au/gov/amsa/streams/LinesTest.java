package au.gov.amsa.streams;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.util.PortFinder;

public class LinesTest {

	@Test
	public void testLinesFromARegularlyEmittingSource()
			throws InterruptedException {
		Observable<String> source = Observable.range(1, 1000000)
				.map(new Func1<Integer, String>() {
					@Override
					public String call(Integer n) {
						return n.toString();
					}
				}).delay(100, TimeUnit.MILLISECONDS, Schedulers.immediate());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		int port = PortFinder.findFreePort();
		final StringServer server = StringServer.create(source, port);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				server.start();
			}
		};
		executor.execute(runnable);
		List<String> list = Lines.from("localhost", port, 1000, 1000).take(5)
				.toList().toBlocking().single();
		System.out.println(list);
		server.stop();
		executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
	}
}
