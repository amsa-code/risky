package au.gov.amsa.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;

public class DriftCandidatesTest {

	private static final double PRECISION = 0.00001;

	@Test
	public void testSplit() {
		String s = "a,,,b,c";
		List<String> items = Arrays.asList(s.split(","));
		assertEquals(Arrays.asList("a", "", "", "b", "c"), items);
	}

	@Test
	public void testReadFromFile() {
		List<DriftCandidate> list = DriftCandidates
		        .fromCsv(new File("src/test/resources/drift-candidates.txt")).toList().toBlocking()
		        .single();
		assertEquals(5, list.size());
		Fix f = list.get(0).fix();
		assertEquals(111450000, f.mmsi());
		assertEquals(-10.518791, f.lat(), PRECISION);
		assertEquals(140.33229, f.lon(), PRECISION);
		assertEquals(1418998619000L, f.time());
		assertEquals(AisClass.B, f.aisClass());
		assertEquals(96.3, f.courseOverGroundDegrees().get(), PRECISION);
		assertEquals(1.0, f.headingDegrees().get(), PRECISION);
		assertEquals(3.5, f.speedOverGroundKnots().get(), PRECISION);
		assertFalse(f.navigationalStatus().isPresent());
		assertEquals(1413956437000L, list.get(0).driftingSince());
	}

}
