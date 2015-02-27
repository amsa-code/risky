package au.gov.amsa.navigation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import au.gov.amsa.risky.format.NavigationalStatus;

public class NavigationalStatusTest {

	@Test
	public void testNavStatusEnumsAreSame() {
		assertEquals(NavigationalStatus.values().length,
				au.gov.amsa.navigation.VesselPosition.NavigationalStatus
						.values().length);
		assertEquals(NavigationalStatus.values().length,
				au.gov.amsa.ais.message.NavigationalStatus
						.values().length);
		for (int i = 0;i<NavigationalStatus.values().length;i++) {
			assertEquals(NavigationalStatus.values()[i].name(), au.gov.amsa.navigation.VesselPosition.NavigationalStatus.values()[i].name());
			assertEquals(NavigationalStatus.values()[i].name(), au.gov.amsa.ais.message.NavigationalStatus.values()[i].name());
		}
	}
	
	
}
