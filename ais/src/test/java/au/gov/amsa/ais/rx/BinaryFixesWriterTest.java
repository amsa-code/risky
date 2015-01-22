package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.ais.rx.BinaryFixesWriter.ByMonth;
import au.gov.amsa.risky.format.Fix;

public class BinaryFixesWriterTest {

	private static final String NMEA_RESOURCE = "/nmea-timestamped.txt";
	private static final int DISTINCT_MMSI = 85;

	@Test
	public void testNumberCraftInTestFile() throws IOException {
		InputStream is = BinaryFixesWriterTest.class
				.getResourceAsStream(NMEA_RESOURCE);
		int count = Streams.extractFixes(Streams.nmeaFrom(is))
				.map(new Func1<Fix, Long>() {
					@Override
					public Long call(Fix fix) {
						return fix.getMmsi();
					}
				}).distinct().count().toBlocking().single();
		assertEquals(DISTINCT_MMSI, count);
		is.close();
	}

	@Test
	public void testBinaryFixesWriterUsingFileMapper() throws IOException {
		InputStream is = BinaryFixesWriterTest.class
				.getResourceAsStream(NMEA_RESOURCE);
		Observable<Fix> fixes = Streams.extractFixes(Streams.nmeaFrom(is));
		String base = "target/binary";
		File directory = new File(base);
		FileUtils.deleteDirectory(directory);
		ByMonth fileMapper = new BinaryFixesWriter.ByMonth(directory);
		BinaryFixesWriter.writeFixes(fileMapper, fixes, 100).subscribe();
		is.close();
		File f = new File(base + File.separator + "2014" + File.separator
				+ "12");
		assertTrue(f.exists());
		assertEquals(85, f.listFiles().length);
	}

}
