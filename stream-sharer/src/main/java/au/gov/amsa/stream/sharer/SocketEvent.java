package au.gov.amsa.stream.sharer;

import com.google.common.base.Optional;

public class SocketEvent {

	private final String host;
	private final int port;
	private final Optional<Throwable> throwable;
	private final SocketEventType event;

	public SocketEvent(String host, int port, Optional<Throwable> throwable,
			SocketEventType event) {
		this.host = host;
		this.port = port;
		this.throwable = throwable;
		this.event = event;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public Optional<Throwable> getThrowable() {
		return throwable;
	}

	public SocketEventType getEvent() {
		return event;
	}

}
