package au.gov.amsa.stream.sharer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class Sockets {

	private static Logger log = LoggerFactory.getLogger(Sockets.class);

	public static Observable<Socket> from(final ServerSocket ss) {
		return Observable
		// accept unlimited connections to server socket
				.range(0, Integer.MAX_VALUE)
				// map to ss
				.map(Functions.constant(ss))
				// report sockets from server socket connections
				.concatMap(new Func1<ServerSocket, Observable<Socket>>() {
					@Override
					public Observable<Socket> call(ServerSocket ss) {
						return Observable.using(socketFactory(ss),
								socketObservable(), socketDispose());
					}
				});

	}

	public static Observable<Socket> from(int serverSocketPort) {
		return ServerSockets.from(serverSocketPort).flatMap(
				new Func1<ServerSocket, Observable<Socket>>() {
					@Override
					public Observable<Socket> call(ServerSocket ss) {
						return from(ss).concatWith(Observable.<Socket> never());
					}
				});
	}

	private static Func0<Socket> socketFactory(final ServerSocket ss) {
		return new Func0<Socket>() {

			@Override
			public Socket call() {
				try {
					return ss.accept();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	private static Func1<Socket, Observable<Socket>> socketObservable() {
		return new Func1<Socket, Observable<Socket>>() {
			@Override
			public Observable<Socket> call(Socket socket) {
				return Observable.just(socket);
			}
		};
	}

	private static Action1<Socket> socketDispose() {
		return new Action1<Socket>() {
			@Override
			public void call(Socket socket) {
				try {
					socket.close();
				} catch (IOException e) {
					// don't care
				}
			}
		};
	}

	public static String toString(Socket socket) {
		StringBuffer s = new StringBuffer();
		s.append(socket.getInetAddress().getHostAddress());
		s.append(":");
		s.append(socket.getPort());
		return s.toString();
	}
}
