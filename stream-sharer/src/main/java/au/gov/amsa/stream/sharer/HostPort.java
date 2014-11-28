package au.gov.amsa.stream.sharer;

public class HostPort {

	private final String host;
	private final int port;

	public HostPort(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String host() {
		return host;
	}

	public int port() {
		return port;
	}

}
