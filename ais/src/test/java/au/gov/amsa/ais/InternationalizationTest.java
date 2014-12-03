package au.gov.amsa.ais;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class InternationalizationTest {

	@Test
	public void testInstantiation() {
		assertNotNull(AisParseException.INVALID_CHARACTER);
		Internationalization.forTestCoverageOnly();
	}
}
