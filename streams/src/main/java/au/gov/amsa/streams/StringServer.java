package au.gov.amsa.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Publishes lines from an Observable<String> source to a ServerSockets.
 */
public class StringServer {

	private static Logger log = LoggerFactory.getLogger(StringServer.class);

	private final ServerSocket ss;
	private volatile boolean keepGoing = true;
	private final Observable<String> source;

	/**
	 * Factory method.
	 * 
	 * @param source
	 *            source to publish on server socket
	 * @param port
	 *            to assign the server socket to
	 */
	public static StringServer create(Observable<String> source, int port) {
		return new StringServer(source, port);
	}

	/**
	 * Constructor.
	 * 
	 * @param ss
	 *            {@link ServerSocket} to publish to
	 * @param source
	 *            the source of lines to publish on ServerSocket
	 */
	public StringServer(Observable<String> source, int port) {
		try {
			this.ss = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.source = source;
	}

	/**
	 * Starts the server. Each connection to the server will bring about another
	 * subscription to the source.
	 */
	public void start() {
		try {
			while (keepGoing) {
				try {
					// this is a blocking call so it hogs a thread
					final Socket socket = ss.accept();
					final String socketName = socket.getInetAddress()
							.getHostAddress() + ":" + socket.getPort();
					try {
						final OutputStream out = socket.getOutputStream();
						source
						// subscribe in background so can accept another
						// connection
						.subscribeOn(Schedulers.io())
						// write each line to the socket OutputStream
								.subscribe(
										createSubscriber(socket, socketName,
												out));
					} catch (IOException e) {
						// could not get output stream (could have closed very
						// quickly after connecting
						// dont' care
					}
				} catch (SocketTimeoutException e) {
					// don't care
				}
			}
		} catch (IOException e) {
			if (keepGoing) {
				log.warn(e.getMessage(), e);
				throw new RuntimeException(e);
			} else
				log.info("server stopped");
		} finally {
			closeServerSocket();
		}
	}

	/**
	 * Stops the server by closing the ServerSocket.
	 */
	public void stop() {
		log.info("stopping");
		keepGoing = false;
		closeServerSocket();
	}

	private void closeServerSocket() {
		try {
			ss.close();
		} catch (IOException e) {
			log.info("could not close server socket: " + e.getMessage());
		}
	}

	private static Subscriber<String> createSubscriber(final Socket socket,
			final String socketName, final OutputStream out) {
		return new Subscriber<String>() {

			@Override
			public void onCompleted() {
				log.info("stream completed");
				closeSocket();
			}

			@Override
			public void onError(Throwable e) {
				log.error(e.getMessage()
						+ " - unexpected due to upstream retry");
				closeSocket();
			}

			@Override
			public void onNext(String line) {
				try {
					out.write(line.getBytes(StandardCharsets.UTF_8));
					out.write('\n');
					out.flush();
				} catch (IOException e) {
					log.info(e.getMessage() + " " + socketName);
					// this will unsubscribe to clean up the
					// resources associated with this subscription
					unsubscribe();
					closeSocket();
				}
			}

			private void closeSocket() {
				try {
					socket.close();
				} catch (IOException e1) {
					log.info("closing socket " + socketName + ":"
							+ e1.getMessage());
				}
			}
		};
	}

}
