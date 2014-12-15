package au.gov.amsa.util.nmea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class NmeaUtilTest {

	private static final String MESSAGE = "GPGSA,A,2,29,19,28,,,,,,,,,,23.4,12.1,20.0";
	private static final String MESSAGE2A = "BSVDM,2,1,5,B,5Cm=9f000000h@q<000m>0H`u8@A8uA@0000000010D2600Ht00000000000,0";
	private static final String MESSAGE2B = "BSVDM,2,2,5,B,00000000008,2";
	private static final String CHECKSUM = "0F";
	private static final String CHECKSUM2 = "13";

	private static final String CHECKSUM_DELIMITER = "*";
	private static final String DOLLAR = "$";
	private static final String EXCLAMATION_POINT = "!";
	private static final String TAG_BLOCK = "\\c:1234567*23\\";

	@Test
	public void testChecksumNoLeadingDollar() {
		assertEquals(CHECKSUM, NmeaUtil.getChecksum(MESSAGE));
	}

	@Test
	public void testChecksumLeadingDollar() {
		assertEquals(CHECKSUM, NmeaUtil.getChecksum(DOLLAR + MESSAGE));
	}

	@Test
	public void testChecksumLeadingExclamation() {
		assertEquals(CHECKSUM,
				NmeaUtil.getChecksum(EXCLAMATION_POINT + MESSAGE));
	}

	@Test
	public void testChecksumTwoLineMessage() {
		assertEquals(CHECKSUM2,
				NmeaUtil.getChecksum(MESSAGE2A + MESSAGE2B, false));
	}

	@Test
	public void testInstantiation() {
		NmeaUtil.forTestCoverageOnly();
	}

	@Test
	public void testIsValid() {
		assertTrue(NmeaUtil.isValid(MESSAGE + CHECKSUM_DELIMITER + CHECKSUM));
	}

	@Test
	public void testTalker() {
		assertEquals(
				Talker.GP,
				NmeaUtil.parseNmea(
						DOLLAR + MESSAGE + CHECKSUM_DELIMITER + CHECKSUM)
						.getTalker());
	}

	@Test
	public void testChecksumCalculationIgnoresTagBlock() {
		assertTrue(NmeaUtil.isValid(TAG_BLOCK + DOLLAR + MESSAGE
				+ CHECKSUM_DELIMITER + CHECKSUM));
	}

	@Test
	public void testIsValidNmeaWhenSentenceHasUnclosedTagBlock() {
		assertFalse(NmeaUtil.isValid("\\"));
	}

	@Test
	public void testIsValidFieldCharacter() {
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 0));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 31));
		assertTrue(NmeaUtil.isValidFieldCharacter((char) 32));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 33));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 36));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 126));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 127));
		assertFalse(NmeaUtil.isValidFieldCharacter((char) 128));
	}

	@Test
	public void testValidCharacterSymbol() {
		assertTrue(NmeaUtil.isValidCharacterSymbol('A'));
		assertTrue(NmeaUtil.isValidCharacterSymbol('a'));
		assertTrue(NmeaUtil.isValidCharacterSymbol('B'));
		assertFalse(NmeaUtil.isValidCharacterSymbol('b'));
	}

	@Test
	public void testGetTalkerGivenNullString() {
		assertNull(NmeaUtil.getTalker(null));
	}

	@Test
	public void testGetTalkerGivenRealTalker() {
		assertEquals(Talker.AB, NmeaUtil.getTalker("AB"));
	}

	@Test
	public void testGetTalkerGivenUnknownTalker() {
		assertEquals(Talker.UNKNOWN, NmeaUtil.getTalker("ZZ"));
	}

	@Test
	public void testGetTalkerDescription() {
		assertEquals("Independent AIS Base Station", NmeaUtil.getTalker("AB")
				.getDescription());
	}

	@Test
	public void testCanCreateNmeaLine() {
		String tag = "\\g:1-2-1536,c:1334258609*2F\\";
		String line = "!BSVDM,2,1,0,A,577V7s02?k61I8Lg<00Dq@E918U<F1=@58000016Op`BL5D8tIm5@PDPCp0T,0*5B";
		List<String> list = createSampleList();

		String result = NmeaUtil.createNmeaLine(null, list);
		assertEquals(line, result);
	}

	private List<String> createSampleList() {
		List<String> list = Lists.newArrayList();
		list.add("!BSVDM");
		list.add("2");
		list.add("1");
		list.add("0");
		list.add("A");
		list.add("577V7s02?k61I8Lg<00Dq@E918U<F1=@58000016Op`BL5D8tIm5@PDPCp0T");
		list.add("0");
		return list;
	}

	@Test
	public void testCreationOfTagBlock() {
		String tag = "\\g:1-2-1536,c:1334258609*2F\\";
		LinkedHashMap<String, String> tags = createSampleTags();
		assertEquals(tag, NmeaUtil.createTagBlock(tags));
	}

	private LinkedHashMap<String, String> createSampleTags() {
		LinkedHashMap<String, String> tags = Maps.newLinkedHashMap();
		tags.put("g", "1-2-1536");
		tags.put("c", "1334258609");
		return tags;
	}

	@Test
	public void testCreationOfNmeaLineFromTagsAndMessage() {
		String tag = "\\g:1-2-1536,c:1334258609*2F\\";
		String line = "!BSVDM,2,1,0,A,577V7s02?k61I8Lg<00Dq@E918U<F1=@58000016Op`BL5D8tIm5@PDPCp0T,0*5B";
		List<String> list = createSampleList();
		LinkedHashMap<String, String> tags = createSampleTags();
		assertEquals(tag + line, NmeaUtil.createNmeaLine(tags, list));
	}

	@Test
	public void testRoundTripOnNmeaMessageOnLineWithTagBlock() {
		String line = "\\g:1-2-1234,s:r3669961,c:1120959341*0D\\!ABVDM,1,1,1,B,…..,0*39";
		NmeaMessage m = NmeaUtil.parseNmea(line);
		assertEquals(line, m.toLine());
	}

	@Test
	public void testSupplementWithTimeDoesNothingToMultilineMessagesAfterFirst() {
		String line = "\\g:2-2-3987*58\\!BSVDM,2,2,0,A,lQ@@0000002,0*00";
		assertEquals(line, NmeaUtil.supplementWithTime(line, 0));
	}

	@Test
	public void testSupplementWithTimeDoesNothingToMessageWithTime() {
		String line = "\\s:rEV02,c:1334337322*5E\\!AIVDM,1,1,,B,14`980002?6UgpR1w0c8cG0L0Gww,0*58";
		assertEquals(line, NmeaUtil.supplementWithTime(line, 0));
	}

	@Test
	public void testSupplementWithTimeAddsTagBlockIfDoesntHaveOne() {
		String line = "$PGHP,1,2012,1,31,5,55,12,0,316,3,316999999,1AIS_S,18*7A";
		assertEquals("\\c:1234567,a:1234567890*1F\\" + line,
				NmeaUtil.supplementWithTime(line, 1234567890));
		assertEquals("69", NmeaUtil.getChecksum("c:1234567"));
	}

	@Test
	public void testSupplementWithTimeInsertsIntoExistingTagBlock() {
		String line = "\\s:rEV02,d:1334337321*5A\\!AIVDM,1,1,,B,33:JeT0OjtVls<;fDlbl5CFH2000,0*71";
		assertEquals(
				"\\s:rEV02,d:1334337321,c:1234567,a:1234567890*69\\!AIVDM,1,1,,B,33:JeT0OjtVls<;fDlbl5CFH2000,0*71",
				NmeaUtil.supplementWithTime(line, 1234567890));
		assertEquals("1F",
				NmeaUtil.getChecksum("s:rEV02,d:1334337321,c:1234567"));
	}

}
