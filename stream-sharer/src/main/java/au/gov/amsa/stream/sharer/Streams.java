package au.gov.amsa.stream.sharer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.StringObservable;

public class Streams {

	public static Observable<String> createSharedReader(HostPort hostPort) {
		return Observable
		// create a stream from a socket and dispose of socket
		// appropriately
				.using(socketCreator(hostPort), observableFactory(),
						socketDisposer())
				// ensure connection has not dropped out by throwing an
				// exception after a minute. This is a good idea with TCPIP
				// because for example a firewall might drop a quiet connection
				// and we won't know about it.
				.timeout(1, TimeUnit.MINUTES)
				// if any exception occurs retry
				.retry()
				// all subscribers use the same stream
				.share();
	}

	private static Func0<Socket> socketCreator(final HostPort hostPort) {
		return new Func0<Socket>() {
			@Override
			public Socket call() {
				try {
					return new Socket(hostPort.host(), hostPort.port());
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
					return StringObservable.split(
							StringObservable.from(reader), "\n");
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
					socket.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

}
