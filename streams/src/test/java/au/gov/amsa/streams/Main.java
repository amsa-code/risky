package au.gov.amsa.streams;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import rx.Observable;

public class Main {

	public static void main(String[] args) {
		Observable<String> lines = StringSockets.from("mariweb.amsa.gov.au")
				.port(9010).quietTimeout(1, TimeUnit.MINUTES)
				.reconnectDelay(1, TimeUnit.SECONDS)
				.charset(StandardCharsets.UTF_8).create();
		final int serverSocketPort = 6564;
		StringServer.create(lines, serverSocketPort).start();
	}
}
