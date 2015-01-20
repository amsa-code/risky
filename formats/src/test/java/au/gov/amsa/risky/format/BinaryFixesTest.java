package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;

public final class BinaryFixesTest {

	private static final double PRECISION = 0.000001;

	@Test
	public void testWriteAndReadBinaryFixes() throws IOException {
		OutputStream os = new BufferedOutputStream(new FileOutputStream(
				"target/123456789.track"));
		Fix fix = new Fix(213456789, -10f, 135f, 1000, of(12), of((short) 1),
				of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f),
				of(46f), AisClass.B);
		byte[] bytes = new byte[BinaryFixes.BINARY_FIX_BYTES];
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		BinaryFixes.write(fix, bb);
		int numFixes = 100000;
		for (int i = 0; i < numFixes; i++)
			os.write(bytes);
		os.close();

		System.out.println("wrote " + numFixes + " fixes");

		long t = System.currentTimeMillis();

		List<Fix> fixes = BinaryFixes.from(new File("target/123456789.track"))
				.toList().toBlocking().single();
		assertEquals(numFixes, fixes.size());
		Fix f = fixes.get(fixes.size() - 1);
		assertEquals(123456789, f.getMmsi());
		assertEquals(-10.0, f.getLat(), PRECISION);
		assertEquals(135, f.getLon(), PRECISION);
		assertEquals(1000, f.getTime(), PRECISION);
		assertEquals(NavigationalStatus.ENGAGED_IN_FISHING, f
				.getNavigationalStatus().get());
		assertEquals(7.5, f.getSpeedOverGroundKnots().get(), PRECISION);
		assertEquals(45, f.getCourseOverGroundDegrees().get(), PRECISION);
		assertEquals(46, f.getHeadingDegrees().get(), PRECISION);
		assertEquals(AisClass.B, f.getAisClass());

		System.out.println("read " + numFixes + " fixes in "
				+ (System.currentTimeMillis() - t) + "ms");
	}

	@Test
	public void testWriteTwoBinaryFixes() throws IOException {
		writeTwoBinaryFixes();
	}

	static void writeTwoBinaryFixes() throws IOException {
		OutputStream os = new BufferedOutputStream(new FileOutputStream(
				"target/123456790.track"));
		long t = 1421708455237L;
		Fix fix1 = new Fix(213456789, -10f, 135f, t, of(12), of((short) 1),
				of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f),
				of(46f), AisClass.B);
		Fix fix2 = new Fix(213456789, -10.1f, 135.2f, t + 1000 * 3600 * 2L,
				of(13), of((short) 2), of(NavigationalStatus.AT_ANCHOR),
				of(4.5f), of(20f), of(30f), AisClass.B);
		BinaryFixes.write(fix1, os);
		BinaryFixes.write(fix2, os);
		os.close();
	}

}
