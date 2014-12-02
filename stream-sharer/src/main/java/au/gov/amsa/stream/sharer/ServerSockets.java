package au.gov.amsa.stream.sharer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ServerSockets {

	private static Logger log = LoggerFactory.getLogger(ServerSockets.class);

	public static Observable<ServerSocket> from(int port) {
		return Observable.using(ssFactory(port), ssObservable(), ssDispose());
	}

	private static Func0<ServerSocket> ssFactory(final int port) {
		return new Func0<ServerSocket>() {

			@Override
			public ServerSocket call() {
				try {
					return new ServerSocket(port);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private static Func1<ServerSocket, Observable<ServerSocket>> ssObservable() {
		return new Func1<ServerSocket, Observable<ServerSocket>>() {

			@Override
			public Observable<ServerSocket> call(ServerSocket ss) {
				return Observable.just(ss).concatWith(
						Observable.<ServerSocket> never());
			}
		};
	}

	private static Action1<ServerSocket> ssDispose() {
		return new Action1<ServerSocket>() {

			@Override
			public void call(ServerSocket ss) {
				try {
					ss.close();
				} catch (IOException e) {
					// don't care
				}
			}
		};
	}

	public static Observable<Void> publish(final Observable<String> source,
			int serverSocketPort) {
		Observable<Socket> sockets = Sockets
		// accept connections on a server socket
				.from(serverSocketPort)
				// process each socket on a different scheduler
				.flatMap(Functions.<Socket> parallel(Schedulers.io()));
		return sockets.flatMap(new Func1<Socket, Observable<Void>>() {
			@Override
			public Observable<Void> call(final Socket socket) {
				source.subscribe(new Subscriber<String>() {

					@Override
					public void onCompleted() {
						// eager unsubscribe
						unsubscribe();
					}

					@Override
					public void onError(Throwable e) {
						// eager unsubscribe
						unsubscribe();
					}

					@Override
					public void onNext(String s) {
						try {
							socket.getOutputStream().write(
									s.getBytes(StandardCharsets.UTF_8));
						} catch (IOException e) {
							log.info("could not write to "
									+ Sockets.toString(socket) + " - "
									+ e.getMessage());
							unsubscribe();
						}
					}
				});
				return Observable.empty();
			}
		});
	}

}
