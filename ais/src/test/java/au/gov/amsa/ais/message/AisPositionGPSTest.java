package au.gov.amsa.ais.message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AisPositionGPSTest {

    private static final double PRECISION = 0.0001;

    @Test
    public void testAisPosition() {
        String m = "Kp15D4pA;hjC1P5h";

        AisPositionGPS p = new AisPositionGPS(m, 0);
        assertEquals(27, p.getMessageId());
        assertEquals(3, p.getRepeatIndicator());
        assertEquals(538006547, p.getMmsi());
        assertEquals(true, p.isHighAccuracyPosition());
        assertEquals(NavigationalStatus.AT_ANCHOR, p.getNavigationalStatus());
        assertEquals(p.getLatitude(), 31.365, PRECISION);
        assertEquals(p.getLongitude(), 32.325, PRECISION);
        assertEquals(0.0, p.getSpeedOverGroundKnots(), PRECISION);
        assertEquals(92, p.getCourseOverGround(), PRECISION);
    }

    @Test
    public void testAisPositionWithSOG() {
        String m = "K9NSI3`3g3P:A7CL";

        AisPositionGPS p = new AisPositionGPS(m, 0);
        assertEquals(27, p.getMessageId());
        assertEquals(0, p.getRepeatIndicator());
        assertEquals(636016910, p.getMmsi());
        assertEquals(true, p.isHighAccuracyPosition());
        assertEquals(NavigationalStatus.UNDER_WAY_USING_ENGINE, p.getNavigationalStatus());
        assertEquals(p.getLatitude(), 2.19, PRECISION);
        assertEquals(p.getLongitude(), 101.9967, PRECISION);
        assertEquals(14, p.getSpeedOverGroundKnots(), PRECISION);
        assertEquals(311, p.getCourseOverGround(), PRECISION);
    }

}
