package au.gov.amsa.ais;

import static au.gov.amsa.ais.ShipTypeDecoder.getShipType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;

import org.junit.Test;

public class ShipTypeDecoderTest {

	@Test
	public void testAll() {
		// for (int i = 0; i < 100; i++)
		// System.out.println(i + "=" + getShipType(i));

		assertEquals("unknown code 0", getShipType(0));
		assertEquals("unknown code 100", getShipType(100));
		assertEquals("Reserved - All", getShipType(10));
		assertEquals("Engaged in diving operations", getShipType(34));
		assertEquals("Tug", getShipType(52));
		assertEquals(
				"WIG - Carrying DG, HS, or MP, IMO Hazard or pollutant category A",
				getShipType(21));

	}

	@Test
	public void testNull() {
		assertNull(getShipType((Integer) null));
	}

	@Test
	public void testBigIntegerNull() {
		assertNull(getShipType((BigInteger) null));
	}

	@Test
	public void testBigIntegerNonNull() {
		assertEquals("unknown code 0", getShipType(BigInteger.valueOf(0)));
	}

	@Test
	public void testInstantiation() {
		ShipTypeDecoder.forTestCoverageOnly();
	}
}
