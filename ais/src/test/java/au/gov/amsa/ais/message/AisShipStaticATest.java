package au.gov.amsa.ais.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AisShipStaticATest {

	private static final double PRECISION = 0.00001;

	@Test
	public void test() {

		String m = "577JNW02BLa=I8`cN20t=@98DE`F0U<h4pB22216C@J>@4M20FlRCp11H2PCQBDSp888880";
		AisShipStaticA s = new AisShipStaticA(m, "source");
		System.out.println(s.toString().replaceAll(",", ",\n"));
		assertEquals(5, s.getMessageId());
		assertEquals(0, s.getRepeatIndicator());
		assertEquals(477535900, s.getMmsi());
		assertEquals(0, s.getAisVersionIndicator());
		assertEquals(9597587, (int) s.getImo().get());
		assertEquals("VRJJ7", s.getCallsign());
		assertEquals("OCTBREEZE ISLAND", s.getName());
		assertEquals(70, s.getShipType());
		assertEquals(154, (int) s.getDimensionA().get());
		assertEquals(26, (int) s.getDimensionB().get());
		assertEquals(14, (int) s.getDimensionC().get());
		assertEquals(16, (int) s.getDimensionD().get());
		assertEquals(180, (int) s.getLengthMetres().get());
		assertEquals(30, (int) s.getWidthMetres().get());
		assertEquals(1, s.getTypeOfElectronicPositionFixingDevice());
		assertEquals(118912, s.getExpectedTimeOfArrivalUnprocessed());
		assertEquals(91.0, s.getMaximumPresentStaticDraughtMetres(), PRECISION);
		assertEquals("RIO DE JANEIRO", s.getDestination());
		assertTrue(s.getDataTerminalAvailable());
		assertEquals(0, s.getSpare());
		assertTrue(s.getDataTerminalAvailable());
		assertEquals("source", s.getSource());
	}
	
	@Test
	public void testDimensionsOnlyNoReferencePoint() {
		String m = "57ldaq@1`57M0u9P000DM>1=E9HETu800000001J00g086u60=@C@SkP000000000000000";
		AisShipStaticA s = new AisShipStaticA(m, "source");
		System.out.println(s.toString().replaceAll(",", ",\n"));
	
		assertFalse(s.getDimensionA().isPresent());
		assertEquals(47, (int) s.getDimensionB().get());
		assertFalse(s.getDimensionC().isPresent());
		assertEquals(8, (int) s.getDimensionD().get());
		assertEquals(47, (int) s.getLengthMetres().get());
		assertEquals(8, (int) s.getWidthMetres().get());
	}
	
	@Test
	public void testDestination() {
		String m = "55DaI402;uLs<H<gV21=@Dhh5:0p5HTL5@u:2216LprDI6Gn0EA0CD2ADc0EDm4PC2@H880";
		AisShipStaticA s = new AisShipStaticA(m, "source");
		assertEquals("DAMPIER,AUSTRALIA", s.getDestination());
	}
	
	@Test
	public void testExpectedTimeOfArrival() {
		assertEquals(1359165600000L, AisShipStaticA.getExpectedTimeOfArrival(2013,1,26,2,0));
	}
}
