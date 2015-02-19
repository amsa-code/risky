package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

public class NetcdfFixesWriterTest {

	@Test
	public void testWriting() {
		Fix f1 = createFix(TimeUnit.DAYS.toMillis(1), -10.5f, 135f);
		Fix f2 = createFix(TimeUnit.DAYS.toMillis(2), -10.8f, 136f);
		List<Fix> fixes = Arrays.asList(f1, f2);
		NetcdfFixesWriter.writeFixes(fixes, new File("target/test.nc"));
	}

	@Test
	@Ignore
	public void testReading() {
		// read nc file and ensure everything makes it across
		// TODO implement reading test
	}

	@Test
	public void testNetcdfConverter() throws IOException {
		TestingUtil.writeTwoBinaryFixes("target/987654321.track");
		int count = NetcdfFixesWriter
				.convertToNetcdf(new File("target"), new File("target/nc"),
						Pattern.compile("987654321.track")).count()
				.toBlocking().single();
		assertEquals(1, count);
	}

	private static Fix createFix(long time, float lat, float lon) {
		return new Fix(213456789, lat, lon, time, of(12), of((short) 1),
				of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f),
				of(46f), AisClass.B);
	}

}
