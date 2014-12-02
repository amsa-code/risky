package au.gov.amsa.streams;

import java.io.IOException;

import au.gov.amsa.streams.Lines;
import au.gov.amsa.streams.StringServer;
import rx.Observable;

public class Main {

	public static void main(String[] args) throws IOException {

		Observable<String> aisLines = Lines.from("mariweb.amsa.gov.au", 9010,
				60000, 1000).map(Lines.TRIM);
		StringServer.start(6564, aisLines);

	}

}
