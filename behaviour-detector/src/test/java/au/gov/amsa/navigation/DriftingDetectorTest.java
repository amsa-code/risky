package au.gov.amsa.navigation;

import static au.gov.amsa.navigation.DriftingDetector.diff;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DriftingDetectorTest {

	private static final double PRECISION = 0.0000001;

	@Test
	public void testDiff() {
		assertEquals(0, diff(0, 0), PRECISION);
	}

	@Test
	public void testDiff2() {
		assertEquals(10, diff(30, 20), PRECISION);
	}

	@Test
	public void testDiff3() {
		assertEquals(10, diff(20, 30), PRECISION);
	}

	@Test
	public void testDiff4() {
		assertEquals(20, diff(350, 10), PRECISION);
	}
	
	@Test
	public void testDiff5() {
		assertEquals(20, diff(10, 350), PRECISION);
	}

}
