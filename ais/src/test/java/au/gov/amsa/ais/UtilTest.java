package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilTest {

	@Test
	public void testPrivateInstantiation() {
		Util.forTestCoverageOnly();
	}

	@Test
	public void testCheckLatLon() {
		Util.checkLatLong(0, 0);
	}

	@Test
	public void testCheckLatLonNegative() {
		Util.checkLatLong(-20, -10);
	}

	@Test
	public void testCheckLatLonPositive() {
		Util.checkLatLong(20, 10);
	}

	@Test(expected = AisParseException.class)
	public void testCheckLatLonLatTooLow() {
		Util.checkLatLong(-90, 0);
	}

	@Test
	public void testCheckLatLonLatAtLimit() {
		Util.checkLatLong(91, 0);
	}

	@Test(expected = AisParseException.class)
	public void testCheckLatLonLatTooHigh() {
		Util.checkLatLong(92, 0);
	}

	@Test(expected = AisParseException.class)
	public void testCheckLatLonLonTooLow() {
		Util.checkLatLong(0, -180);
	}

	@Test
	public void testCheckLatLonLonAtLimit() {
		Util.checkLatLong(0, 181);
	}

	@Test(expected = AisParseException.class)
	public void testCheckLatLonLonTooHigh() {
		Util.checkLatLong(0, 182);
	}

	@Test(expected = AisParseException.class)
	public void testCheckMessageIdThrowsAisParseExceptionWhenMessageIdsNotEqual() {
		Util.checkMessageId(0, AisMessageType.POSITION_REPORT_ASSIGNED);
	}

	@Test
	public void testCheckMessageId() {
		Util.checkMessageId(AisMessageType.POSITION_REPORT_ASSIGNED.getId(),
				AisMessageType.POSITION_REPORT_ASSIGNED);
	}

	@Test(expected = AisParseException.class)
	public void testCheckMessageIdShouldThrowExceptionIfListEmpty() {
		Util.checkMessageId(AisMessageType.POSITION_REPORT_ASSIGNED.getId());
	}

	@Test
	public void testCheckMessageIdInMultiple() {
		Util.checkMessageId(AisMessageType.POSITION_REPORT_ASSIGNED.getId(),
				AisMessageType.POSITION_REPORT_ASSIGNED,
				AisMessageType.POSITION_REPORT_CLASS_B);
	}

	@Test
	public void testCheckMessageIdInMultipleChangedOrder() {
		Util.checkMessageId(AisMessageType.POSITION_REPORT_ASSIGNED.getId(),
				AisMessageType.POSITION_REPORT_ASSIGNED,
				AisMessageType.POSITION_REPORT_CLASS_B);
	}

	@Test(expected = AisParseException.class)
	public void testCheckMessageIdNotInMultiple() {
		Util.checkMessageId(AisMessageType.POSITION_REPORT_SPECIAL.getId(),
				AisMessageType.POSITION_REPORT_ASSIGNED,
				AisMessageType.POSITION_REPORT_CLASS_B);
	}

	@Test
	public void areEqualTestEquality() {
		assertTrue(Util.areEqual(1, 1));
	}

	@Test
	public void areEqualTestInequality() {
		assertFalse(Util.areEqual(1, 2));
	}

	@Test(expected = AisParseException.class)
	public void testAscii8To6BitBinaryIllegalCharacterLowThrowsException() {
		Util.ascii8To6bitBin(new byte[] { 0 });
	}

	@Test(expected = AisParseException.class)
	public void testAscii8To6BitBinaryIllegalCharacterHighThrowsException() {
		Util.ascii8To6bitBin(new byte[] { 120 });
	}

	@Test(expected = AisParseException.class)
	public void testAscii8To6BitBinaryIllegalCharacterMediumThrowsException() {
		Util.ascii8To6bitBin(new byte[] { 88 });
	}

	@Test
	public void testIsClassAPositionReport() {
		assertTrue(Util.isClassAPositionReport(1));
		assertTrue(Util.isClassAPositionReport(2));
		assertTrue(Util.isClassAPositionReport(3));
		assertFalse(Util.isClassAPositionReport(4));
	}

	@Test
	public void testConvert6BitToAscii() {
		Byte value = Util.convert6BitCharToStandardAscii((byte) 65);
		assertTrue(value.equals((byte) 0));
	}
	
	@Test
	public void testLeastPowerOf2Given0Returns1() {
		assertEquals(1,Util.leastPowerOf2GreaterThanOrEqualTo(0));
	}
	
	@Test
	public void testLeastPowerOf2Given1Returns1() {
		assertEquals(1,Util.leastPowerOf2GreaterThanOrEqualTo(1));
	}
	
	@Test
	public void testLeastPowerOf2Given2() {
		assertEquals(2,Util.leastPowerOf2GreaterThanOrEqualTo(2));
	}
	
	@Test
	public void testLeastPowerOf2Given3() {
		assertEquals(4,Util.leastPowerOf2GreaterThanOrEqualTo(3));
	}
	
	@Test
	public void testLeastPowerOf2Given127() {
		assertEquals(128,Util.leastPowerOf2GreaterThanOrEqualTo(127));
	}
}
