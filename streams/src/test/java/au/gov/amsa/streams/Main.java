package au.gov.amsa.streams;

import java.io.IOException;

import rx.Observable;

public class Main {

	public static void main(String[] args) throws IOException {

		String host = "mariweb.amsa.gov.au";
		int port = 9010;
		long quietTimeoutMs = 60000;
		long reconnectDelayMs = 1000;
		int serverSocketPort = 6564;

		Observable<String> lines = Lines.from(host, port, quietTimeoutMs,
				reconnectDelayMs).map(Lines.TRIM);
		StringServer.start(lines, serverSocketPort);

	}

}
