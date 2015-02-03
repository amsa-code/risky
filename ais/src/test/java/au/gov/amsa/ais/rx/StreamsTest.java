package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

public class StreamsTest {

	private static final double PRECISION = 0.0000001;

	@Test
	public void testExtract() throws IOException {
		InputStream is = StreamsTest.class
				.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extract(Streams.nmeaFrom(is)).count().toBlocking()
				.single();
		assertEquals(200, count);
		is.close();
	}

	@Test
	public void testExtractFixesGetsMultiple() throws IOException {
		InputStream is = StreamsTest.class
				.getResourceAsStream("/exact-earth-with-tag-block.txt");
		int count = Streams.extractFixes(Streams.nmeaFrom(is)).count()
				.toBlocking().single();
		assertEquals(178, count);
		is.close();
	}

	@Test
	public void testExtractFixFromAisPositionA() throws IOException {
		InputStream is = new ByteArrayInputStream(
				"\\s:rEV02,c:1334337317*58\\!AIVDM,1,1,,B,19NWuLhuRb5QHfCpPcwj`26B0<02,0*5F"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking()
				.single();
		assertEquals(636091763, fix.getMmsi());
		assertEquals(-13.0884285, fix.getLat(), PRECISION);
		assertEquals(1334337317000L, fix.getTime());
		assertEquals(AisClass.A, fix.getAisClass());
		assertEquals(67.0, fix.getHeadingDegrees().get(), PRECISION);
		assertEquals(67.199996948, fix.getCourseOverGroundDegrees().get(),
				PRECISION);
		assertFalse(fix.getLatencySeconds().isPresent());
		assertEquals(1, (int) fix.getSource().get());
		System.out.println(fix);
		is.close();
	}

	@Test
	public void testExtractFixGetsNavigationalStatusOfUnderWay()
			throws IOException {

		InputStream is = new ByteArrayInputStream(
				"\\s:Pt Hedland NOMAD,c:1421878430*19\\!ABVDM,1,1,,B,33m2SV800K`Nh85lJdeDlTCN0000,0*1A"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking()
				.single();
		assertEquals(NavigationalStatus.UNDER_WAY, fix.getNavigationalStatus()
				.get());
		is.close();
	}

	@Test
	public void testExtractFixGetsNavigationalStatusOfAbsentForNotDefined15()
			throws IOException {

		InputStream is = new ByteArrayInputStream(
				"\\s:ISEEK Flinders,c:1421879177*61\\!AIVDO,1,1,,,1>qc9wwP009qrbKd6DMAPww>0000,0*76"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking()
				.single();
		assertFalse(fix.getNavigationalStatus().isPresent());
		is.close();
	}

	@Test
	public void testNmeaFromFile() {
		assertEquals(
				100,
				(int) Streams
						.nmeaFrom(
								new File(
										"src/test/resources/nmea-timestamped.txt"))
						.count().toBlocking().single());
	}

	@Test
	public void testExtractFixFromAisPositionB() throws IOException {
		InputStream is = new ByteArrayInputStream(
				"\\s:MSQ - Mt Cootha,c:1421877742*76\\!AIVDM,1,1,,B,B7P?oe00FRg9t`L4T4IV;wbToP06,0*2F"
						.getBytes(Charset.forName("UTF-8")));
		Fix fix = Streams.extractFixes(Streams.nmeaFrom(is)).toBlocking()
				.single();
		assertEquals(503576500, fix.getMmsi());
		assertEquals(-27.46356391906, fix.getLat(), PRECISION);
		assertEquals(1421877742000L, fix.getTime());
		assertEquals(AisClass.B, fix.getAisClass());
		assertEquals(BinaryFixes.SOURCE_PRESENT_BUT_UNKNOWN, (int) fix
				.getSource().get());
		assertFalse(fix.getNavigationalStatus().isPresent());
		assertFalse(fix.getLatencySeconds().isPresent());
		System.out.println(fix);
		is.close();
	}

	// @Test
	public void testMerge() {
		Observable<Integer> a = Observable.create(new OnSubscribe<Integer>() {

			@Override
			public void call(Subscriber<? super Integer> child) {
				for (int i = 1; i <= 1000; i++) {
					if (child.isUnsubscribed())
						return;
					child.onNext(i);
				}
				child.onCompleted();
			}
		}).subscribeOn(Schedulers.computation());
		int count = a.mergeWith(a).count().toBlocking().single();
		assertEquals(2000, count);
	}
}
