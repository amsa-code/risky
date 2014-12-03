package au.gov.amsa.ais;

import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import au.gov.amsa.ais.rx.Streams;

public class Main {

	public static void main(String[] args) throws IOException {
		// System.setProperty("many", "/home/dxm/satellite-ais-messages.txt");
		// new NmeaStreamProcessorIntegrationTest().testManyMany();
		if (args.length >= 2) {
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			final String type;
			if (args.length >= 3)
				type = args[2];
			else
				type = "raw";
			final Observable<?> stream;
			if ("extract".equals(type))
				stream = Streams.connectAndExtract(host, port);
			else
				stream = Streams.connect(host, port);
			stream.toBlocking().forEach(new Action1<Object>() {
				@Override
				public void call(Object object) {
					System.out.println(object);
				}
			});
		}
	}
}
