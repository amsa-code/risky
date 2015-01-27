package au.gov.amsa.streams;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringsTest {

	@Test
	public void testTrim() {
		assertEquals("trimmed", Strings.TRIM.call("  \ttrimmed\r\n   "));
	}

	@Test
	public void testTrimOnNullInputReturnsNull() {
		assertEquals(null, Strings.TRIM.call(null));
	}

}
