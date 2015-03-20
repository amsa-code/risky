package au.gov.amsa.ais.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AisMessageOtherTest {

	@Test
	public void testMethods() {
		AisMessageOther m = new AisMessageOther(3, 0);
		assertEquals(3, m.getMessageId());
		assertNull(m.getSource());
	}

	@Test
	public void testAisMessageOtherReturnsSource() {
		AisMessageOther m = new AisMessageOther(3, "boo", 0);
		assertEquals("boo", m.getSource());
	}

	@Test
	public void testToString() {
		new AisMessageOther(3, "boo", 0).toString();
	}

}
