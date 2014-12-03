package au.gov.amsa.streams;

import java.nio.charset.StandardCharsets;

import rx.Observable;

public class Main {

	public static void main(String[] args) {

		Observable<String> lines = Lines.from("mariweb.amsa.gov.au").port(9010)
				.quietTimeoutMs(60000).reconnectDelayMs(1000)
				.charset(StandardCharsets.UTF_8).create();

		final int serverSocketPort = 6564;
		StringServer.create(lines, serverSocketPort).start();
	}
}
