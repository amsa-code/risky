package au.gov.amsa.stream.sharer;

import java.io.IOException;

import rx.Observable;
import rx.schedulers.Schedulers;

public class Main {

	public static void main(String[] args) throws IOException {

		Observable<String> aisLines = Lines.from("mariweb.amsa.gov.au", 9010,
				60000, 1000).map(Lines.TRIM);
		// aisLines.lift(
		// Logging.<String> logger().showCount().every(100).showValue()
		// .log()).subscribe();

		// new StringServer(6564, aisLines).start();
		ServerSockets.publish(aisLines, 6564).observeOn(Schedulers.immediate())
				.subscribe();
	}

}
