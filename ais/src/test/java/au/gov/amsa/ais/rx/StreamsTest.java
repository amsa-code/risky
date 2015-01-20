package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Test;

import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.nmea.NmeaUtil;

public class StreamsTest {

	private static final double PRECISION = 0.0000001;

	@Test
	public void testExtract() throws IOException {
		InputStream is = StreamsTest.class
				.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extract(NmeaUtil.nmeaLines(is)).count()
				.toBlocking().single();
		assertEquals(200, count);
		is.close();
	}

	@Test
	public void testExtractFixFromAisPositionA() throws IOException {
		InputStream is = new ByteArrayInputStream(
				"\\s:rEV02,c:1334337317*58\\!AIVDM,1,1,,B,19NWuLhuRb5QHfCpPcwj`26B0<02,0*5F"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(NmeaUtil.nmeaLines(is)).toBlocking()
				.single();
		assertEquals(636091763, fix.getMmsi());
		assertEquals(-13.0884285, fix.getLat(), PRECISION);
		assertEquals(1334337317000L, fix.getTime());
		assertEquals(AisClass.A, fix.getAisClass());
		System.out.println(fix);
		is.close();
	}

	@Test
	public void testExtractFixFromAisPositionB() throws IOException {
		InputStream is = new ByteArrayInputStream(
				"\\s:rEV02,c:1334337317*58\\!AIVDM,1,1,,B,19NWuLhuRb5QHfCpPcwj`26B0<02,0*5F"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(NmeaUtil.nmeaLines(is)).toBlocking()
				.single();
		assertEquals(636091763, fix.getMmsi());
		assertEquals(-13.0884285, fix.getLat(), PRECISION);
		assertEquals(1334337317000L, fix.getTime());
		assertEquals(AisClass.A, fix.getAisClass());
		System.out.println(fix);
		is.close();
	}
}
