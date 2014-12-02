package au.gov.amsa.stream.sharer;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

public class Main {

	private static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws IOException {

		Observable<String> aisLines = Lines.from("mariweb.amsa.gov.au", 9010,
				60000, 1000).map(Lines.TRIM);
		// aisLines.lift(
		// Logging.<String> logger().showCount().every(100).showValue()
		// .log()).subscribe();

		new StringServer(new ServerSocket(6564), aisLines).start();
	}
}
