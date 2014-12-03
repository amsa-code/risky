package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import au.gov.amsa.util.nmea.NmeaUtil;

public class StreamsTest {

	@Test
	public void testExtract() throws IOException {
		InputStream is = StreamsTest.class
				.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extract(NmeaUtil.nmeaLines(is)).count()
				.toBlocking().single();
		assertEquals(200, count);
		is.close();
	}
}
