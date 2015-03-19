package au.gov.amsa.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SixBitTest {

	@Test
	public void testGetValueFirstBitSet() {
		boolean[] bits = { true, true, false };
		assertEquals(6, SixBit.getValue(0, 3, bits));
	}

	@Test
	public void testGetValueFirstBitNotSet() {
		boolean[] bits = { false, true, false };
		assertEquals(2, SixBit.getValue(0, 3, bits));
	}

	@Test
	public void testGetSignedValueFirstBitSet() {
		boolean[] bits = { true, true, false };
		assertEquals(-2, SixBit.getSignedValue(0, 3, bits));
	}

	@Test
	public void testGetSignedValueFirstBitNotSet() {
		boolean[] bits = { false, true, false };
		assertEquals(2, SixBit.getSignedValue(0, 3, bits));
	}

	@Test
	public void testGetValueZero() {
		boolean[] bits = { false, false, false };
		assertEquals(0, SixBit.getValue(0, 3, bits));
	}

	@Test
	public void testGetSignedValueZero() {
		boolean[] bits = { false, false, false };
		assertEquals(0, SixBit.getSignedValue(0, 3, bits));
	}

}
