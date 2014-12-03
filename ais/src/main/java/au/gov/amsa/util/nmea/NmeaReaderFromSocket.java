package au.gov.amsa.util.nmea;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

/**
 * Reads NMEA messages from a {@link Socket}.
 * 
 * @author dxm
 * 
 */
public class NmeaReaderFromSocket implements NmeaReader {

	private static Logger log = Logger.getLogger(NmeaReaderFromSocket.class);
	private final Socket socket;

	/**
	 * Constructor.
	 * 
	 * @param host
	 * @param port
	 */
	public NmeaReaderFromSocket(String host, int port) {
		this(createSocket(host, port));
	}

	private static Socket createSocket(String host, int port) {
		try {
			return new Socket(host, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@VisibleForTesting
	NmeaReaderFromSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public Iterable<String> read() {
		try {
			return NmeaUtil.nmeaLines(socket.getInputStream()).toBlocking()
					.toIterable();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				log.warn(e.getMessage(), e);
			}
	}
}