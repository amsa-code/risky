package au.gov.amsa.util.nmea;

import org.junit.Test;

public class NmeaMessageParserExceptionTest {

	@Test
	public void testConstructorWithThrowable() {
		new NmeaMessageParseException(new Exception());
	}
}
