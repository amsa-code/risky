package au.gov.amsa.streams;

import static rx.Observable.just;
import static rx.Observable.range;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public final class StringSockets {

	private static final Logger log = LoggerFactory
			.getLogger(StringSockets.class);

	/**
	 * Returns an Observable sequence of lines from the given host and port. If
	 * the stream is quiet for <code>quietTimeoutMs</code> then a reconnect will
	 * be attempted. Note that this is a good idea with TCPIP connections as for
	 * instance a firewall can simply drop a quiet connection without the client
	 * being aware of it. If any exception occurs a reconnect will be attempted
	 * after <code>reconnectDelayMs</code>. If the socket is closed by the
	 * server (the end of the input stream is reached) then a reconnect is
	 * attempted after <code>reconnectDelayMs</code>.
	 * 
	 * @param hostPort
	 * @return Observable sequence of lines (including possible new line
	 *         character at end if exists).
	 */
	public static Observable<String> from(final String host, final int port,
			long quietTimeoutMs, long reconnectDelayMs, Charset charset) {
		// delay connect by delayMs so that if server closes
		// stream on every connect we won't be in a mad loop of
		// failing connections
		return triggersWithDelay(reconnectDelayMs)
		// log (note connection number will increase if socket is cleanly closed
		// by the server)
				.lift(Logging.<Integer> logger()
						.onNextPrefix("connectionNumber=").log())
				// connect to server and read lines from its input stream
				.concatMap(from(host, port, charset))
				// ensure connection has not dropped out by throwing an
				// exception after a minute of no messages. This is a good idea
				// with TCPIP because for example a firewall might drop a quiet
				// connection and we won't know about it.
				.timeout(quietTimeoutMs, TimeUnit.MILLISECONDS)
				// if any exception occurs retry
				.retry()
				// all subscribers use the same stream
				.share();
	}

	public static class Builder {
		private final String host;
		private int port = 6564;
		private long quietTimeoutMs = 60000;
		private long reconnectDelayMs = 30000;
		private Charset charset = StandardCharsets.UTF_8;

		Builder(String host) {
			this.host = host;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder quietTimeoutMs(long quietTimeoutMs) {
			this.quietTimeoutMs = quietTimeoutMs;
			return this;
		}

		public Builder quietTimeout(long duration, TimeUnit unit) {
			return quietTimeoutMs(unit.toMillis(duration));
		}

		public Builder reconnectDelayMs(long reconnectDelayMs) {
			this.reconnectDelayMs = reconnectDelayMs;
			return this;
		}

		public Builder reconnectDelay(long duration, TimeUnit unit) {
			return reconnectDelayMs(unit.toMillis(duration));
		}

		public Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		public Observable<String> create() {
			return from(host, port, quietTimeoutMs, reconnectDelayMs, charset);
		}

	}

	/**
	 * Returns a builder for converting a socket read to an Observable. Defaults
	 * to port=6564, quietTimeoutMs=60000, reconnectDelayMs=30000, charset=
	 * UTF-8.
	 * 
	 * @param host
	 * @return
	 */
	public static Builder from(String host) {
		return new Builder(host);
	}

	/**
	 * Returns a <i>synchronous</i>s stream of a count of integers with delayMs
	 * between emissions.
	 * 
	 * @param delayMs
	 * @return stream of integers starting at 1 with delayMs between emissions
	 */
	private static Observable<Integer> triggersWithDelay(long delayMs) {
		return just(1)
		// keep counting
				.concatWith(
				// numbers from 2 on now with delay
						range(2, Integer.MAX_VALUE - 1)
						// delay till next number released
								.delay(delayMs, TimeUnit.MILLISECONDS,
										Schedulers.immediate()));
	}

	private static Func1<Integer, Observable<String>> from(final String host,
			final int port, final Charset charset) {
		return new Func1<Integer, Observable<String>>() {
			@Override
			public Observable<String> call(Integer n) {
				return Ob
				// create a stream from a socket and dispose of socket
				// appropriately
						.using(socketCreator(host, port),
								socketObservableFactory(charset),
								socketDisposer(), true)
						// cannot ask host to slow down so buffer on
						// backpressure
						.onBackpressureBuffer();
			}
		};
	}

	@VisibleForTesting
	static Func0<Socket> socketCreator(final String host, final int port) {
		return new Func0<Socket>() {
			@Override
			public Socket call() {
				try {
					log.info("creating socket to " + host + ":" + port);
					return new Socket(host, port);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@VisibleForTesting
	static Func1<Socket, Observable<String>> socketObservableFactory(
			final Charset charset) {
		return new Func1<Socket, Observable<String>>() {

			@Override
			public Observable<String> call(Socket socket) {
				try {
					return StringObservable.from(new InputStreamReader(socket
							.getInputStream(), charset));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	@VisibleForTesting
	static Action1<Socket> socketDisposer() {
		return new Action1<Socket>() {
			@Override
			public void call(Socket socket) {
				try {
					log.info("closing socket "
							+ socket.getInetAddress().getHostAddress() + ":"
							+ socket.getPort());
					socket.close();
				} catch (IOException e) {
					// don't really care if socket could not be closed cleanly
					log.info(e.getMessage(), e);
				}
			}
		};
	}

	public static Observable<String> mergeLinesFrom(
			Collection<HostPort> hostPorts) {
		Preconditions.checkArgument(hostPorts.size() > 0);
		List<Observable<String>> sources = new ArrayList<Observable<String>>();
		for (HostPort hostPort : hostPorts) {
			Observable<String> o =
			// connect to localhost
			from(hostPort.getHost())
			// connect to port
					.port(hostPort.getPort())
					// if quiet then reconnect
					.quietTimeoutMs(hostPort.getQuietTimeoutMs())
					// reconnect delay
					.reconnectDelayMs(hostPort.getReconnectDelayMs())
					// create
					.create();
			Observable<String> lines = Strings.split(o, "\n").subscribeOn(
					Schedulers.io());
			sources.add(lines);
		}
		return Observable.merge(sources);
	}
}
