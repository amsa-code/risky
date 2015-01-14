package au.gov.amsa.ais;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import rx.functions.Func1;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.nmea.NmeaReader;

public class NmeaReaderFromInputStream implements NmeaReader {

	private static final Func1<String, Boolean> NON_EMPTY = new Func1<String,Boolean>(){
		@Override
		public Boolean call(String s) {
			return s.trim().length()>0;
		}};
		
	private final InputStream is;

	public NmeaReaderFromInputStream(InputStream is) {
		this.is = is;
	}

	@Override
	public Iterable<String> read() {
		return Strings
				.split(Strings.from(new InputStreamReader(is, Charset
						.forName("UTF-8"))), "\n").filter(NON_EMPTY).toBlocking().toIterable();
	}
}
