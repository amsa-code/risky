package au.gov.amsa.streams;

public class HostPort {
	private final String host;
	private final int port;
	private final long quietTimeoutMs;
	private final long reconnectDelayMs;

	public HostPort(String host, int port, long quietTimeoutMs,
			long reconnectDelayMs) {
		this.host = host;
		this.port = port;
		this.quietTimeoutMs = quietTimeoutMs;
		this.reconnectDelayMs = reconnectDelayMs;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public long getQuietTimeoutMs() {
		return quietTimeoutMs;
	}

	public long getReconnectDelayMs() {
		return reconnectDelayMs;
	}

	public static HostPort create(String host, int port, long quietTimeoutMs,
			long reconnectDelayMs) {
		return new HostPort(host, port,quietTimeoutMs, reconnectDelayMs);
	}

}
