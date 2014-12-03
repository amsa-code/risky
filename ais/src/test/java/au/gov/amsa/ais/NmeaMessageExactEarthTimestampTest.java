package au.gov.amsa.ais;

import static au.gov.amsa.ais.NmeaMessageExactEarthTimestamp.isExactEarthTimestamp;
import static au.gov.amsa.ais.NmeaMessageExactEarthTimestamp.isPghp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

import au.gov.amsa.util.nmea.NmeaMessageParseException;

public class NmeaMessageExactEarthTimestampTest {

	@Test
	public void testParseTimestampMessage() {
		String line = "$PGHP,1,2004,12,21,23,59,58,999,219,219000001,219000002,1,6D*56";
		NmeaMessageExactEarthTimestamp m = new NmeaMessageExactEarthTimestamp(
				line);
		Calendar cal = Calendar.getInstance(Util.TIME_ZONE_UTC);
		cal.setTimeInMillis(m.getTime());
		assertEquals(2004, cal.get(Calendar.YEAR));
		assertEquals(12, cal.get(Calendar.MONTH) + 1);
		assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(59, cal.get(Calendar.MINUTE));
		assertEquals(58, cal.get(Calendar.SECOND));
		assertEquals(999, cal.get(Calendar.MILLISECOND));
		assertEquals("6D", m.getFollowingSequenceChecksum());

	}

	@Test(expected = NmeaMessageParseException.class)
	public void testInvalidTimestampMessageConstructorThrowsParserException() {
		new NmeaMessageExactEarthTimestamp("$PGHP,1,2");
	}

	@Test(expected = AisParseException.class)
	public void testInvalidTimestampMessageConstructorThrowsParserExceptionWhenChecksumNotInColumn13() {
		new NmeaMessageExactEarthTimestamp(
				"$PGHP,1,2004,12,21,23,59,58,999,219,219000001,219000002,6D*56");
	}

	@Test(expected = NmeaMessageParseException.class)
	public void testInvalidTimestampMessageConstructorThrowsParserExceptionWithOneColumnOnly() {
		new NmeaMessageExactEarthTimestamp("$PGHP");
	}

	@Test(expected = AisParseException.class)
	public void testInvalidAisNmeaThrowsNmeaParserException() {
		new NmeaMessageExactEarthTimestamp(
				"$PGHP,2,1,1,,0201250608494E54455256414C02020168*1E");
	}

	@Test
	public void testNmeaMessageWhenTagBlockNotClosedProperly() {
		String line = "\\g:1-2-1234,s:r3669961,n:4,r:1120959341,some stuff here";
		assertFalse(isExactEarthTimestamp(line));
	}

	@Test
	public void testExactEarthTimestampWhenNotEnoughColumns() {
		String line = "$PGHP,2,1,1,,02011E06035549440600060453554944060006045555494406267B42373135393341332D334145432D344141372D394436352D3037433941344138464644377D0603554E410618637573745F65655F696E7465726E616C5F646973706C61790603414354020104*68";
		assertFalse(isExactEarthTimestamp(line));
	}

	@Test
	public void testIsPghpLineIsFalseGivenNull() {
		assertFalse(isPghp(null));
	}

	@Test
	public void testIsPghpLineIsFalseGivenBlank() {
		assertFalse(isPghp(""));
	}

	@Test
	public void testIsPghpLineIsFalseGivenNonBlankString() {
		assertFalse(isPghp("abcde"));
	}

	@Test
	public void testIsPghpLineIsTrueWhenHasTagBlock() {
		assertTrue(isPghp("\\hello\\$PGHP"));
	}

	@Test
	public void testIsPghpLineIsTrueWhenHasNoTagBlock() {
		assertTrue(isPghp("$PGHP,boo"));
	}

	@Test
	public void testIsPghpLineIsTrueWhenHasPghpNotAtBeginning() {
		assertFalse(isPghp("hi$PGHP,boo"));
	}
}
