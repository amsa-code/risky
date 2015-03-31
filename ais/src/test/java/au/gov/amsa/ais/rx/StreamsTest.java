package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesWriter;
import au.gov.amsa.risky.format.BinaryFixesWriter.ByMonth;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

import com.google.common.base.Optional;

public class StreamsTest {

	private static final double PRECISION = 0.0000001;

	@Test
	public void testExtract() throws IOException {
		InputStream is = StreamsTest.class.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extract(Streams.nmeaFrom(is)).count().toBlocking().single();
		assertEquals(200, count);
		is.close();
	}

	@Test
	public void testExtractFixesGetsMultiple() throws IOException {
		InputStream is = StreamsTest.class.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extractFixes(Streams.nmeaFrom(is)).count().toBlocking().single();
		assertEquals(178, count);
		is.close();
	}

	@Test
	public void testExtractFixFromAisPositionA() throws IOException {
		InputStream is = new ByteArrayInputStream(
		        "\\s:rEV02,c:1334337317*58\\!AIVDM,1,1,,B,19NWuLhuRb5QHfCpPcwj`26B0<02,0*5F"
		                .getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking().single();
		assertEquals(636091763, fix.mmsi());
		assertEquals(-13.0884285, fix.lat(), PRECISION);
		assertEquals(1334337317000L, fix.time());
		assertEquals(AisClass.A, fix.aisClass());
		assertEquals(67.0, fix.headingDegrees().get(), PRECISION);
		assertEquals(67.199996948, fix.courseOverGroundDegrees().get(), PRECISION);
		assertFalse(fix.latencySeconds().isPresent());
		assertEquals(1, (int) fix.source().get());
		System.out.println(fix);
		is.close();
	}

	@Test
	public void testExtractFixGetsNavigationalStatusOfUnderWay() throws IOException {

		InputStream is = new ByteArrayInputStream(
		        "\\s:Pt Hedland NOMAD,c:1421878430*19\\!ABVDM,1,1,,B,33m2SV800K`Nh85lJdeDlTCN0000,0*1A"
		                .getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking().single();
		assertEquals(NavigationalStatus.UNDER_WAY, fix.navigationalStatus().get());
		is.close();
	}

	@Test
	public void testExtractFixGetsNavigationalStatusOfAbsentForNotDefined15() throws IOException {

		InputStream is = new ByteArrayInputStream(
		        "\\s:ISEEK Flinders,c:1421879177*61\\!AIVDO,1,1,,,1>qc9wwP009qrbKd6DMAPww>0000,0*76"
		                .getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking().single();
		assertTrue(fix.navigationalStatus().isPresent());
		assertEquals(Optional.of(NavigationalStatus.NOT_DEFINED), fix.navigationalStatus());
		is.close();
	}

	@Test
	public void testNmeaFromFile() {
		assertEquals(100,
		        (int) Streams.nmeaFrom(new File("src/test/resources/nmea-timestamped.txt")).count()
		                .toBlocking().single());
	}

	@Test
	public void testExtractFixFromAisPositionB() throws IOException {
		InputStream is = new ByteArrayInputStream(
		        "\\s:MSQ - Mt Cootha,c:1421877742*76\\!AIVDM,1,1,,B,B7P?oe00FRg9t`L4T4IV;wbToP06,0*2F"
		                .getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking().single();
		assertEquals(503576500, fix.mmsi());
		assertEquals(-27.46356391906, fix.lat(), PRECISION);
		assertEquals(1421877742000L, fix.time());
		assertEquals(AisClass.B, fix.aisClass());
		assertEquals(BinaryFixes.SOURCE_PRESENT_BUT_UNKNOWN, (int) fix.source().get());
		assertFalse(fix.navigationalStatus().isPresent());
		assertFalse(fix.latencySeconds().isPresent());
		System.out.println(fix);
		is.close();
	}

	private static final String NMEA_RESOURCE = "/nmea-timestamped.txt";
	private static final int DISTINCT_MMSI = 85;

	@Test
	public void testNumberCraftInTestFile() throws IOException {
		InputStream is = StreamsTest.class.getResourceAsStream(NMEA_RESOURCE);
		int count = Streams.extractFixes(Streams.nmeaFrom(is)).map(new Func1<Fix, Long>() {
			@Override
			public Long call(Fix fix) {
				return fix.mmsi();
			}
		}).distinct().count().toBlocking().single();
		assertEquals(DISTINCT_MMSI, count);
		is.close();
	}

	@Test
	public void testBinaryFixesWriterUsingFileMapper() throws IOException {
		InputStream is = StreamsTest.class.getResourceAsStream(NMEA_RESOURCE);
		Observable<Fix> fixes = Streams.extractFixes(Streams.nmeaFrom(is));
		String base = "target/binary";
		File directory = new File(base);
		FileUtils.deleteDirectory(directory);
		ByMonth fileMapper = new BinaryFixesWriter.ByMonth(directory);
		BinaryFixesWriter.writeFixes(fileMapper, fixes, 100, false).subscribe();
		is.close();
		File f = new File(base + File.separator + "2014" + File.separator + "12");
		assertTrue(f.exists());
		assertEquals(85, f.listFiles().length);
	}

	public static void main(String[] args) {
		System.out.println((byte) 128);
	}
}
