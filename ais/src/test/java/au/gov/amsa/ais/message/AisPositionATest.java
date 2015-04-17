package au.gov.amsa.ais.message;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.Communications;

public class AisPositionATest {

    private static final double PRECISION = 0.0001;;

    @Test
    public void testAisPosition() {
        String m = "15MgK45P3@G?fl0E`JbR0OwT0@MS";

        AisPositionA p = new AisPositionA(m, 0);
        assertEquals(1, p.getMessageId());
        assertEquals(0, p.getRepeatIndicator());
        assertEquals(366730000, p.getMmsi());
        assertEquals(NavigationalStatus.MOORED, p.getNavigationalStatus());
        assertEquals(null, p.getRateOfTurn());
        assertEquals(20.8, p.getSpeedOverGroundKnots(), PRECISION);
        assertFalse(p.isHighAccuracyPosition());
        assertEquals(p.getLatitude(), 37.80380333333333, PRECISION);
        assertEquals(p.getLongitude(), -122.392531666667, PRECISION);
        assertEquals(p.getCourseOverGround(), 51.3, PRECISION);
        assertEquals(null, p.getTrueHeading());
        assertEquals(p.getTimeSecondsOnly(), 50);
        assertFalse(p.isUsingRAIM());
        assertEquals(0, p.getSpecialManoeuvreIndicator());
        assertEquals(0, p.getSpare());
        Communications comms = p.getCommunications();
        System.out.println(comms);
        assertNotNull(comms);
        assertEquals(0, comms.getSyncState());
        assertEquals(4, comms.getSlotTimeout());
        assertEquals(1891, (int) comms.getSlotNumber());
        assertNull(comms.getReceivedStations());
        assertNull(comms.getMinutesUtc());
        assertNull(comms.getSlotOffset());
        assertNull(p.getSource());

        // System.out.println(p.toString().replaceAll(",", ",\n"));
    }

    @Test
    public void testCommicationsWithTime() {
        String m = "177KQJ5000G?tO`K>RA1wUbN0TKH";
        AisPositionA p = new AisPositionA(m, 0);
        Communications comms = p.getCommunications();
        assertEquals(1, comms.getSyncState());
        assertEquals(1, comms.getSlotTimeout());
        assertEquals(234, (int) comms.getMinutesUtc());
        // System.out.println(p.toString().replaceAll(",", ",\n"));
    }

    @Test
    public void testNullCommunicationsTime() {
        String m = "17P;1:0P00bKEn5nDKg9cOw82@De";
        AisPositionA p = new AisPositionA(m, 0);
        Communications comms = p.getCommunications();
        assertNull(comms.getMinuteUtc());
        assertNull(comms.getHourUtc());
        assertNull(comms.getMinutesUtc());
    }

    @Test
    public void testConstructorWithMessageAndSource() {
        String m = "15MgK45P3@G?fl0E`JbR0OwT0@MS";
        AisPositionA p = new AisPositionA(m, "boo", 0);
        assertEquals("boo", p.getSource());
    }

    @Test(expected = AisParseException.class)
    public void testConstructorFailsWhenLineHasWrongMessageId() {
        String m = "B7P@fj00RJVpbIuUhlF93wm5WP06";
        new AisPositionA(m, 0);
    }

    @Test
    public void testExtractRateOfTurn() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(-128).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionA.extractRateOfTurn(ex);
        assertEquals(null, heading);
    }

    @Test
    public void testExtractTrueHeading() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(511).atLeastOnce();
        replay(ex);
        Integer heading = AisPositionA.extractTrueHeading(ex);
        assertEquals(null, heading);
    }

    @Test
    public void testExtractCourseOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(3600).atLeastOnce();
        replay(ex);
        Double cog = AisPositionA.extractCourseOverGround(ex);
        assertEquals(null, cog);
    }

    @Test
    public void testExtractSpeedOverGround() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getValue(anyInt(), anyInt())).andReturn(1023).atLeastOnce();
        replay(ex);
        Double sog = AisPositionA.extractSpeedOverGround(ex);
        assertEquals(null, sog);
    }

    @Test
    public void testExtractLongitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(108600000).atLeastOnce();
        replay(ex);
        Double longitude = AisPositionA.extractLongitude(ex);
        assertEquals(null, longitude);
    }

    @Test
    public void testExtractLatitude() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(54600000).atLeastOnce();
        replay(ex);
        Double latitude = AisPositionA.extractLatitude(ex);
        assertEquals(null, latitude);
    }

    @Test
    public void testExtractLongitudeBadPos() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(118600000).atLeastOnce();
        replay(ex);
        assertNull(AisPositionA.extractLongitude(ex));
    }

    @Test
    public void testExtractLatitudeBadPos() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(58600000).atLeastOnce();
        replay(ex);
        assertNull(AisPositionA.extractLatitude(ex));

    }

    @Test
    public void testExtractLongitudeBadNeg() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(-118600000).atLeastOnce();
        replay(ex);
        assertNull(AisPositionA.extractLongitude(ex));
    }

    @Test
    public void testExtractLatitudeBadNeg() {
        AisExtractor ex = createMock(AisExtractor.class);
        expect(ex.getSignedValue(anyInt(), anyInt())).andReturn(-58600000).atLeastOnce();
        replay(ex);
        assertNull(AisPositionA.extractLatitude(ex));
    }

}
