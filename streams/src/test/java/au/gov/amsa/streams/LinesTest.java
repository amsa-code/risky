package au.gov.amsa.streams;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LinesTest {

	@Test
	public void testTrim() {
		assertEquals("trimmed", Strings.TRIM.call("  \ttrimmed\r\n   "));
	}

	@Test
	public void testTrimOnNullInputReturnsNull() {
		assertEquals(null, Strings.TRIM.call(null));
	}

	@Test(expected = RuntimeException.class)
	public void testSocketCreatorThrowsException() {
		StringSockets.socketCreator("non-existent-host", 1234).call();
	}

}
