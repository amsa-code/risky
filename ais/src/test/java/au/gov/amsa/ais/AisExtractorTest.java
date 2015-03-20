package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AisExtractorTest {

	@Test
	public void testExtractorGetsMessageId() {
		String m = "1G72VO0335bPmqcabrJan7rl0000";
		assertEquals(1, new AisExtractor(m, 0, 0).getMessageId());
	}

	@Test(expected = AisParseException.class)
	public void testExtractorConstructorThrowsAisParseExceptionIfMessageNotLongEnough() {
		String m = "1G72VO0335bPmqcabrJan7rl0000";
		new AisExtractor(m, 10000);
	}

	@Test
	public void testExtractorConstructorInstantiatedIfMessageLongEnough() {
		String m = "1G72VO0335bPmqcabrJan7rl0000";
		new AisExtractor(m, 1, 0);
	}

	@Test(expected = AisParseException.class)
	public void testStringNotLongEnoughThrowsAisParseExceptionUsingGetString() {
		String m = "";
		AisExtractor ex = new AisExtractor(m, 10, 0);
		ex.getString(1, 2);
	}

	@Test(expected = AisParseException.class)
	public void testStringNotLongEnoughtThrowsAisParseExceptionUsingGetValue() {
		String m = "";
		AisExtractor ex = new AisExtractor(m, 10, 0);
		ex.getSignedValue(1, 2);
	}
}
