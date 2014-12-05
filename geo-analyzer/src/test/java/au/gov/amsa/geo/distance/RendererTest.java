package au.gov.amsa.geo.distance;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;

import org.junit.Test;

public class RendererTest {

	@Test
	public void test() {
		assertEquals("1.23E-11",
				new DecimalFormat("0.##E0").format(0.00000000001234567));
	}
}
