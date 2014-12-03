package au.gov.amsa.ais;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import rx.observables.StringObservable;
import au.gov.amsa.util.nmea.NmeaReader;

public class NmeaReaderFromInputStream implements NmeaReader {

	private final InputStream is;

	public NmeaReaderFromInputStream(InputStream is) {
		this.is = is;
	}

	@Override
	public Iterable<String> read() {
		return StringObservable
				.split(StringObservable.from(new InputStreamReader(is, Charset
						.forName("UTF-8"))), "\n").toBlocking().toIterable();
	}
}
