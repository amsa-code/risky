package au.gov.amsa.ais.message;

import static com.google.common.base.Optional.of;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;

public class AisPositionBExtendedTest {

    private static final double PRECISION = 0.00001;

    @Test
    public void testParser() {
        // String line =
        // "!AIVDM,1,1,,B,C5N3SRgPEnJGEBT>NhWAwwo862PaLELTBJ:V00000000S0D:R220,0*0B";
        String line = "C5N3SRgPEnJGEBT>NhWAwwo862PaLELTBJ:V00000000S0D:R220";
        AisPositionBExtended m = new AisPositionBExtended(line, 0);
        assertEquals(19, m.getMessageId());
        assertEquals(0, m.getRepeatIndicator());
        assertEquals(367059850, m.getMmsi());
        assertEquals(8.7, m.getSpeedOverGroundKnots(), PRECISION);
        assertFalse(m.isHighAccuracyPosition());
        assertEquals(-88.8103916667, m.getLongitude(), PRECISION);
        assertEquals(29.543695, m.getLatitude(), PRECISION);
        assertEquals(335.9, m.getCourseOverGround(), PRECISION);
        assertEquals(null, m.getTrueHeading());
        assertEquals(46, m.getTimeSecondsOnly());
        assertEquals("CAPT.J.RIMES", m.getName());
        assertEquals(70, m.getShipType());
        assertEquals(of(5), m.getDimensionA());
        assertEquals(of(21), m.getDimensionB());
        assertEquals(of(4), m.getDimensionC());
        assertEquals(of(4), m.getDimensionD());
        assertEquals(of(26), m.getLengthMetres());
        assertEquals(of(8), m.getWidthMetres());
        assertNull(m.getSource());

        // System.out.println(m);
    }

    @Test
    public void testExtractTrueHeadingNotAvailiable() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(511).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionBExtended.extractTrueHeading(ex);
        assertEquals(null, heading);
    }

    @Test
    public void testExtractTrueHeadingOk() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(236).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionBExtended.extractTrueHeading(ex);
        assertEquals(236, heading.intValue());
    }

    @Test
    public void testExtractCourseOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(3600).atLeastOnce();
        replay(ex);
        Double cog = AisPositionBExtended.extractCourseOverGround(ex);
        assertEquals(null, cog);
    }

    @Test
    public void testExtractSpeedOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(1023).atLeastOnce();
        replay(ex);
        Double sog = AisPositionBExtended.extractSpeedOverGround(ex);
        assertEquals(null, sog);
    }

    @Test
    public void testExtractLongitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(108600000).atLeastOnce();
        replay(ex);
        Double longitude = AisPositionBExtended.extractLongitude(ex);
        assertEquals(null, longitude);
    }

    @Test
    public void testExtractLatitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(54600000).atLeastOnce();
        replay(ex);
        Double latitude = AisPositionBExtended.extractLatitude(ex);
        assertEquals(null, latitude);
    }

}
