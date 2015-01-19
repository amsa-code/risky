package au.gov.amsa.risky.format;

import java.io.IOException;

import org.junit.Test;

public class RenjinTest {

	@Test
	public void testRenjin()  {
		Renjin.call("/test.r");
	}
	
	
}
