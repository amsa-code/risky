package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NetcdfFixesWriterTest {

	@Test
	public void testWriting() {
		Fix f1 = createFix(TimeUnit.DAYS.toMillis(1), -10.5f, 135f);
		Fix f2 = createFix(TimeUnit.DAYS.toMillis(2), -10.8f, 136f);
		List<Fix> fixes = Arrays.asList(f1, f2);
		NetcdfFixesWriter.writeFixes(fixes, new File("target/test.nc"));
	}

	private static Fix createFix(long time, float lat, float lon) {
		return new Fix(213456789, lat, lon, time, of(12), of((short) 1),
				of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f),
				of(46f), AisClass.B);
	}

}
