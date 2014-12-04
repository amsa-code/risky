package au.gov.amsa.streams;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
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
		List<String> list = Strings
				.from("localhost", port, 1000, 1000, StandardCharsets.UTF_8)
				.take(5).toList().toBlocking().single();
		System.out.println(list);
		server.stop();
		executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testTrim() {
		assertEquals("trimmed", Strings.TRIM.call("  \ttrimmed\r\n   "));
	}

	@Test
	public void testTrimOnNullInputReturnsNull() {
		assertEquals(null, Strings.TRIM.call(null));
	}

	@Test(expected = RuntimeException.class)
	public void testSocketCreatorThrowsException() {
		Strings.socketCreator("non-existent-host", 1234).call();
	}

	@Test(expected = RuntimeException.class)
	public void testSocketObservableFactoryOnException() throws IOException {
		Socket socket = mock(Socket.class);
		doThrow(new IOException("hi")).when(socket).getInputStream();
		Strings.socketObservableFactory(StandardCharsets.UTF_8).call(socket);
	}

	@Test
	public void testSocketDisposerOnException() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress address = mock(InetAddress.class);
		doReturn(address).when(socket).getInetAddress();
		doReturn("ahost").when(address).getHostAddress();
		doReturn(1234).when(socket).getPort();
		doThrow(new IOException("hi")).when(socket).close();
		Strings.socketDisposer().call(socket);
	}

	private static class MySource implements OnSubscribe<String> {

		private final String value;

		public MySource(String value) {
			this.value = value;
		}

		@Override
		public void call(Subscriber<? super String> sub) {
			while (!sub.isUnsubscribed()) {
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					// do nothing
				}
				sub.onNext(value);
			}
		}

	}

	public static void main(String[] args) {
		Observable.create(new MySource("a"))
				.subscribeOn(Schedulers.newThread())
				.mergeWith(Observable.create(new MySource("b")))
				.subscribe(new Action1<String>() {

					@Override
					public void call(String s) {
						if (!s.equals("a"))
							System.out.println(s);
					}
				});
	}
}
