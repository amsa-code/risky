package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import au.gov.amsa.ais.message.AisAidToNavigation;
import au.gov.amsa.ais.message.AisBStaticDataReportPartA;
import au.gov.amsa.ais.message.AisBStaticDataReportPartB;
import au.gov.amsa.ais.message.AisBaseStation;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisPositionB;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;

public class AisMessageParserTest {

    @Test
    public void testAisPositionA() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("15MgK45P3@G?fl0E`JbR0OwT0@MS", 0);

        assertTrue(m instanceof AisPositionA);
    }

    @Test
    public void testAisBaseStation() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("403OviQuMGCqWrRO9>E6fE700@GO", 0);

        assertTrue(m instanceof AisBaseStation);
    }

    @Test
    public void testAisShipStaticA() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse(
                "577JNW02BLa=I8`cN20t=@98DE`F0U<h4pB22216C@J>@4M20FlRCp11H2PCQBDSp888880", 0);

        assertTrue(m instanceof AisShipStaticA);
    }

    @Test
    public void testAisPositionB() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("B7P@fj00RJVpbIuUhlF93wm5WP06", 0);

        assertTrue(m instanceof AisPositionB);
    }

    @Test
    public void testBExtended() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("C5N3SRgPEnJGEBT>NhWAwwo862PaLELTBJ:V00000000S0D:R220", 0);

        assertTrue(m instanceof AisPositionBExtended);
    }

    @Test
    public void testParseAtonMessage() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("E>lsp0;`bRb:0h97QUP00000000E6LE2ttVw020@@@P020", 0);

        assertTrue(m instanceof AisAidToNavigation);
        AisAidToNavigation a = (AisAidToNavigation) m;
        // System.out.println(a);
        assertEquals(21, a.getMessageId());
        assertEquals(0, a.getRepeatIndicator());
        assertEquals(995031040, a.getMmsi());
        assertEquals(23, a.getAtoNType());
        assertEquals("QUETTA ROCK", a.getName());
        assertEquals("", a.getAtonStatus());
        assertTrue(a.isHighAccuracyPosition());
    }
    
    @Test
    public void testStaticDataReportPartA() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("H7PH6A15DDDr1=@5:0f22222220", 0);

        assertTrue(m instanceof AisBStaticDataReportPartA);
    }
    
    @Test
    public void testStaticDataReportPartB() {
        AisMessageParser p = new AisMessageParser();
        AisMessage m = p.parse("H7PJ@:4UCBD6f6<57hhh001H5220", 0);

        assertTrue(m instanceof AisBStaticDataReportPartB);
    }

    @Test
    public void test() {
        String bit = "55CgN202=iPc<D5CP01JpfpD@@TD00000000001AS@jLN5j`1NjjFSk@P@0000000000002";
        AisMessageParser p = new AisMessageParser();
        AisMessage a = p.parse(bit, 0);
        // System.out.println(a);
    }

}
