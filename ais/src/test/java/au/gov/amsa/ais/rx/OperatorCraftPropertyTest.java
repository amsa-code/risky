package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.ais.rx.operators.OperatorCraftProperty;
import au.gov.amsa.util.nmea.NmeaUtil;

public class OperatorCraftPropertyTest {

	@Test
	public void testCraftPropertyOperator() throws IOException {
		InputStream is = StreamsTest.class
				.getResourceAsStream("/exact-earth-with-tag-block.txt");
		List<CraftProperty> list = Streams
				.extractMessages(NmeaUtil.nmeaLines(is))
				.lift(new OperatorCraftProperty()).toList().toBlocking()
				.single();
		assertEquals(CraftPropertyName.CALLSIGN, list.get(0).getName());
		assertEquals(new Mmsi(566206000), list.get(0).getMmsi());
		assertEquals("9V9115", list.get(0).getValue());
		assertEquals(CraftPropertyName.DESTINATION, list.get(1).getName());
		assertEquals(new Mmsi(566206000), list.get(1).getMmsi());
		assertEquals("PDM BRAZIL", list.get(1).getValue());
		System.out.println(list.size());
		is.close();
	}
}
