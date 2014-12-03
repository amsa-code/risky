package au.gov.amsa.ais;

import java.util.List;

import au.gov.amsa.util.nmea.NmeaReader;

public class NmeaReaderFromArray implements NmeaReader {

	private final List<String> list;

	public NmeaReaderFromArray(List<String> list) {
		this.list = list;
	}

	@Override
	public Iterable<String> read() {
		return list;
	}

}
