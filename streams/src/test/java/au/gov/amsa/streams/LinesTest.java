package au.gov.amsa.streams;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
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

	@Test
	public void testTrim() {
		assertEquals("trimmed", Lines.TRIM.call("  \ttrimmed\r\n   "));
	}

	@Test
	public void testTrimOnNullInputReturnsNull() {
		assertEquals(null, Lines.TRIM.call(null));
	}

	@Test(expected = RuntimeException.class)
	public void testSocketCreatorThrowsException() {
		Lines.socketCreator("non-existent-host", 1234).call();
	}

	@Test(expected = RuntimeException.class)
	public void testSocketObservableFactoryOnException() throws IOException {
		Socket socket = mock(Socket.class);
		doThrow(new IOException("hi")).when(socket).getInputStream();
		Lines.socketObservableFactory().call(socket);
	}

	@Test
	public void testSocketDisposerOnException() throws IOException {
		Socket socket = mock(Socket.class);
		InetAddress address = mock(InetAddress.class);
		doReturn(address).when(socket).getInetAddress();
		doReturn("ahost").when(address).getHostAddress();
		doReturn(1234).when(socket).getPort();
		doThrow(new IOException("hi")).when(socket).close();
		Lines.socketDisposer().call(socket);
	}
}
