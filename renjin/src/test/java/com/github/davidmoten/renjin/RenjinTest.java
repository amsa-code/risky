package com.github.davidmoten.renjin;

import org.junit.Test;

public class RenjinTest {

	@Test
	public void testExcecutionOfRCode() {
		Renjin.execute(RenjinTest.class.getResourceAsStream("/test.r"));
	}

}
