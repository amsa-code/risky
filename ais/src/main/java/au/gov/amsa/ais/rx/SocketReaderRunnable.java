package au.gov.amsa.ais.rx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observer;

public class SocketReaderRunnable implements Runnable {

	private static Logger log = LoggerFactory.getLogger(SocketReaderRunnable.class);

	private final AtomicBoolean keepGoing = new AtomicBoolean(true);
	private final AtomicReference<Socket> socket = new AtomicReference<Socket>(
			null);
	private final Observer<? super String> observer;

	private final AtomicReference<BufferedReader> reader = new AtomicReference<BufferedReader>();
	private final Object lock = new Object();

	private final HostPort hostPort;

	public SocketReaderRunnable(HostPort hostPort,
			Observer<? super String> observer) {
		this.hostPort = hostPort;
		this.observer = observer;
	}

	@Override
	public void run() {
		try {
			log.info("creating new socket");
			synchronized (lock) {
				socket.set(createSocket(hostPort.getHost(), hostPort.getPort()));
			}
			log.info("waiting one second before attempting connect");
			Thread.sleep(1000);
			InputStream is = socket.get().getInputStream();
			BufferedReader br;
			synchronized (lock) {
				br = new BufferedReader(new InputStreamReader(is));
				reader.set(br);
			}
			while (keepGoing.get()) {
				final String line;
				synchronized (lock) {
					if (keepGoing.get())
						line = br.readLine();
					else
						line = null;
				}
				if (line != null)
					try {
						observer.onNext(line);
					} catch (RuntimeException e) {
						log.warn(e.getMessage(), e);
					}
				else
					keepGoing.set(false);
			}
			observer.onCompleted();
			log.info("completed");
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			observer.onError(e);
		}
	}

	public void cancel() {
		log.info("cancelling socket read");
		synchronized (lock) {
			keepGoing.set(false);
			// only allow socket to be closed once because a fresh
			// instance of Socket could have been opened to the same host and
			// port and we don't want to mess with it.
			if (socket.get() != null) {
				if (reader.get() != null)
					try {
						reader.get().close();
					} catch (IOException e) {
						// ignore
					}
				try {
					socket.get().close();
					socket.set(null);
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public boolean isCancelled() {
		return keepGoing.get();
	}

	private static Socket createSocket(final String host, final int port) {
		try {
			return new Socket(host, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
