package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import au.gov.amsa.ais.message.AisAidToNavigation;
import au.gov.amsa.ais.message.AisBaseStation;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisPositionB;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;

public class AisMessageParserTest {

	@Test
	public void testAisPositionA() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p.parse("15MgK45P3@G?fl0E`JbR0OwT0@MS");

		assertTrue(m instanceof AisPositionA);
	}

	@Test
	public void testAisBaseStation() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p.parse("403OviQuMGCqWrRO9>E6fE700@GO");

		assertTrue(m instanceof AisBaseStation);
	}

	@Test
	public void testAisShipStaticA() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p
				.parse("577JNW02BLa=I8`cN20t=@98DE`F0U<h4pB22216C@J>@4M20FlRCp11H2PCQBDSp888880");

		assertTrue(m instanceof AisShipStaticA);
	}

	@Test
	public void testAisPositionB() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p.parse("B7P@fj00RJVpbIuUhlF93wm5WP06");

		assertTrue(m instanceof AisPositionB);
	}

	@Test
	public void testBExtended() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p
				.parse("C5N3SRgPEnJGEBT>NhWAwwo862PaLELTBJ:V00000000S0D:R220");

		assertTrue(m instanceof AisPositionBExtended);
	}

	@Test
	public void testParseAtonMessage() {
		AisMessageParser p = new AisMessageParser();
		AisMessage m = p
				.parse("E>lsp0;`bRb:0h97QUP00000000E6LE2ttVw020@@@P020");

		assertTrue(m instanceof AisAidToNavigation);
		AisAidToNavigation a = (AisAidToNavigation) m;
		System.out.println(a);
		assertEquals(21, a.getMessageId());
		assertEquals(0, a.getRepeatIndicator());
		assertEquals(995031040, a.getMmsi());
		assertEquals(23, a.getAtoNType());
		assertEquals("QUETTA ROCK", a.getName());
		assertEquals("", a.getAtonStatus());
		assertTrue(a.isHighAccuracyPosition());
	}

	@Test
	public void test() {
		String bit = "55CgN202=iPc<D5CP01JpfpD@@TD00000000001AS@jLN5j`1NjjFSk@P@0000000000002";
		AisMessageParser p = new AisMessageParser();
		AisMessage a = p.parse(bit);
		System.out.println(a);
	}

}
