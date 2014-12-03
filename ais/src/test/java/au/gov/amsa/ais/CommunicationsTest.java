package au.gov.amsa.ais;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CommunicationsTest {

	@Test
	public void testGetReceivedStationsTimeout3() {
		testGetReceivedStationsTimeout(3);
	}

	@Test
	public void testGetReceivedStationsTimeout5() {
		testGetReceivedStationsTimeout(5);
	}

	@Test
	public void testGetReceivedStationsTimeout7() {
		testGetReceivedStationsTimeout(7);
	}

	@Test
	public void testGetReceivedStationsTimeoutOther() {
		AisExtractor ex = createMock(AisExtractor.class);
		replay(ex);
		assertNull(Communications.getReceivedStations(ex, 2, 10));
		verify(ex);
	}

	@Test
	public void testSlotNumberTimeout2() {
		testGetSlotNumberTimeout(2);
	}

	@Test
	public void testGetSlotNumberTimeout4() {
		testGetSlotNumberTimeout(4);
	}

	@Test
	public void testGetSlotNumberTimeout6() {
		testGetSlotNumberTimeout(6);
	}

	@Test
	public void testGetSlotNumberTimeoutOther() {
		AisExtractor ex = createMock(AisExtractor.class);
		replay(ex);
		assertNull(Communications.getSlotNumber(ex, 3, 10));
		verify(ex);
	}

	private void testGetReceivedStationsTimeout(int slotTimeout) {
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(10).atLeastOnce();
		replay(ex);
		assertEquals(10,
				(int) Communications.getReceivedStations(ex, slotTimeout, 10));
		verify(ex);
	}

	private void testGetSlotNumberTimeout(int slotTimeout) {
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(10).atLeastOnce();
		replay(ex);
		assertEquals(10,
				(int) Communications.getSlotNumber(ex, slotTimeout, 10));
		verify(ex);
	}
}
