package au.gov.amsa.ais.message;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.Communications;

public class AisPositionBTest {

    private static final double PRECISION = 0.00001;;

    @Test
    public void test() {
        String s = "B7P@fj00RJVpbIuUhlF93wm5WP06";
        AisPositionB p = new AisPositionB(s, 0);

        // System.out.println(insertNewLines(p));
        assertEquals(AisMessageType.POSITION_REPORT_CLASS_B.getId(), p.getMessageId());
        assertEquals(0, p.getRepeatIndicator());
        assertEquals(503590600, p.getMmsi());
        assertEquals(0, p.getSpare());
        assertEquals(13.7, p.getSpeedOverGroundKnots(), PRECISION);
        assertTrue(p.isHighAccuracyPosition());
        assertEquals(145.826645, p.getLongitude(), PRECISION);
        assertEquals(-16.846818333333335, p.getLatitude(), PRECISION);
        assertEquals(219.2, p.getCourseOverGround(), PRECISION);
        assertEquals(null, p.getTrueHeading());
        assertEquals(42, p.getTimeSecondsOnly());
        assertEquals(0, p.getSpare2());
        assertFalse(p.isSotdmaUnit());
        assertFalse(p.isEquippedWithIntegratedDisplayForMessages12And14());
        assertTrue(p.isEquippedWithDscFunction());
        assertFalse(p.isStationOperatingInAssignedMode());
        assertTrue(p.canOperateOverWholeMarineBand());
        assertFalse(p.canManageFrequenciesViaMessage22());
        assertTrue(p.isUsingRAIM());
        assertTrue(p.isITDMACommunicationState());
        assertFalse(p.isSotdmaUnit());
        Communications comms = p.getCommunications();
        assertEquals(3, comms.getSyncState());
        assertEquals(0, comms.getSlotTimeout());
        assertNull(comms.getReceivedStations());
        assertNull(comms.getSlotNumber());
        assertNull(comms.getMinutesUtc());
        assertEquals(6, (int) comms.getSlotOffset());
        assertNull(p.getSource());

    }

    @Test
    public void testIncorrectMessageId() {
        AisExtractorFactory factory = createMock(AisExtractorFactory.class);
        AisExtractor ex = createMock(AisExtractor.class);
        String message = "";
        expect(factory.create(message, AisBaseStation.MIN_LENGTH, 0)).andReturn(ex).once();
        expect(ex.getSignedValue(107, 134)).andReturn(0).anyTimes();
        expect(ex.getSignedValue(79, 107)).andReturn(0).anyTimes();
        expect(ex.getMessageId()).andReturn(0).atLeastOnce();
        replay(factory, ex);

        try {
            new AisBaseStation(factory, "", "", 0);
            fail();
        } catch (AisParseException e) {
            // expected
        }
        verify(factory);
    }

    @Test
    public void testExtractTrueHeadingNotAvailiable() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(511).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionB.extractTrueHeading(ex);
        assertEquals(null, heading);
    }

    @Test
    public void testExtractTrueHeadingOk() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(236).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionB.extractTrueHeading(ex);
        assertEquals(236, heading.intValue());
    }

    @Test
    public void testExtractCourseOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(3600).atLeastOnce();
        replay(ex);
        Double cog = AisPositionB.extractCourseOverGround(ex);
        assertEquals(null, cog);
    }

    @Test
    public void testExtractSpeedOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(1023).atLeastOnce();
        replay(ex);
        Double sog = AisPositionB.extractSpeedOverGround(ex);
        assertEquals(null, sog);
    }

    @Test
    public void testExtractLongitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(108600000).atLeastOnce();
        replay(ex);
        Double longitude = AisPositionB.extractLongitude(ex);
        assertEquals(null, longitude);
    }

    @Test
    public void testExtractLatitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(54600000).atLeastOnce();
        replay(ex);
        Double latitude = AisPositionB.extractLatitude(ex);
        assertEquals(null, latitude);
    }

}
