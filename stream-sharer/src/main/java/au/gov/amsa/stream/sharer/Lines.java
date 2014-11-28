package au.gov.amsa.stream.sharer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;

import com.github.davidmoten.rx.slf4j.Logging;

public class Lines {

	private static final Logger log = LoggerFactory.getLogger(Lines.class);

	/**
	 * Returns an Observable sequence of lines from the given host and port. If
	 * the stream is quiet for one minute then a reconnect will occur. If any
	 * exception occurs a reconnect will occur after the given retryInterval.
	 * 
	 * @param hostPort
	 * @return
	 */
	public static Observable<String> from(final String host, final int port,
			long quietTimeoutMs, long reconnectDelayMs) {
		return Observable
		// if the server closes the stream we want to just connect again
				.range(1, Integer.MAX_VALUE)
				// delay connect by one second so that if server closes stream
				// on every connect we won't be in a mad loop of failing
				// connections
				.delay(reconnectDelayMs, TimeUnit.MILLISECONDS,
						Schedulers.immediate())
				// log
				.lift(Logging.<Integer> logger().onNextPrefix("n=").log())
				// connect to server and read lines from its input stream
				.concatMap(streamFrom(host, port))
				// ensure connection has not dropped out by throwing an
				// exception after a minute. This is a good idea with TCPIP
				// because for example a firewall might drop a quiet connection
				// and we won't know about it.
				.timeout(quietTimeoutMs, TimeUnit.MILLISECONDS)
				// if any exception occurs retry
				.retry()
				// all subscribers use the same stream
				.share();
	}

	private static Func1<Integer, Observable<String>> streamFrom(
			final String host, final int port) {
		return new Func1<Integer, Observable<String>>() {
			@Override
			public Observable<String> call(Integer n) {
				return Observable
				// create a stream from a socket and dispose of socket
				// appropriately
						.using(socketCreator(host, port), observableFactory(),
								socketDisposer());
			}
		};
	}

	private static Func0<Socket> socketCreator(final String host, final int port) {
		return new Func0<Socket>() {
			@Override
			public Socket call() {
				try {
					log.info("creating socket to " + host + ":" + port);
					return new Socket(host, port);
				} catch (UnknownHostException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private static Func1<Socket, Observable<String>> observableFactory() {
		return new Func1<Socket, Observable<String>>() {

			@Override
			public Observable<String> call(Socket socket) {
				try {
					InputStream is = socket.getInputStream();
					InputStreamReader reader = new InputStreamReader(is,
							"UTF-8");
					return StringObservable.from(reader);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private static Action1<Socket> socketDisposer() {
		return new Action1<Socket>() {
			@Override
			public void call(Socket socket) {
				try {
					log.info("closing socket");
					socket.close();
				} catch (IOException e) {
					// don't really care if socket could not be closed cleanly
					log.warn(e.getMessage(), e);
				}
			}
		};
	}

}
