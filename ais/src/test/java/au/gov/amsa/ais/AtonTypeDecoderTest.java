package au.gov.amsa.ais;

import static au.gov.amsa.ais.AtonTypeDecoder.getAtonType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;

import org.junit.Test;

public class AtonTypeDecoderTest {

	@Test
	public void testAll() {
		// for (int i = 0; i < 32; i++)
		// System.out.println(i + "=" + getAtonType(i));

		assertEquals("Not specified", getAtonType(0));
		assertEquals("unknown code 100", getAtonType(100));
		assertEquals("Fixed - Light, without sectors", getAtonType(5));
		assertEquals("Fixed - Beacon, Cardinal E", getAtonType(10));
		assertEquals("Floating - Light Vessel, LANBY, Rigs", getAtonType(31));
		assertEquals("Fixed - Beacon, Isolated danger", getAtonType(17));
		assertEquals("Fixed - Beacon, Special mark", getAtonType(19));
		assertEquals("Floating - Cardinal Mark N", getAtonType(20));
		assertEquals("Floating - Cardinal Mark E", getAtonType(21));

	}

	@Test
	public void testNull() {
		assertNull(getAtonType((Integer) null));
	}

	@Test
	public void testBigIntegerNull() {
		assertNull(getAtonType((BigInteger) null));
	}

	@Test
	public void testBigIntegerNonNull() {
		assertEquals("Not specified", getAtonType(BigInteger.valueOf(0)));
	}

	@Test
	public void testInstantiation() {
		AtonTypeDecoder.forTestCoverageOnly();
	}
}
