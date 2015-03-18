package au.gov.amsa.ais.message;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.Util;

public class AisBaseStationTest {

	private static final double PRECISION = 0.00001;;

	@Test
	public void testNormalInstantionPassesChecks() {
		String line = "403OviQuMGCqWrRO9>E6fE700@GO";
		AisBaseStation m = new AisBaseStation(line, 0);
		assertEquals(4, m.getMessageId());
		assertEquals(0, m.getRepeatIndicator());
		assertEquals(3669702, m.getMmsi());
		assertEquals(2007, m.getYear());
		assertEquals(5, m.getMonth());
		assertEquals(14, m.getDay());
		assertEquals(19, m.getHour());
		assertEquals(57, m.getMinute());
		assertEquals(39, m.getSecond());
		assertEquals("2007-05-14 19:57:39.000Z", formatAsUtc(m.getTimestamp()));
		assertEquals(1, m.getPositionAccuracy());
		assertEquals(-76.35236166666666666666666667, m.getLongitude(), PRECISION);
		assertEquals(36.88376666666666666666666667, m.getLatitude(), PRECISION);
		assertNull(m.getSource());
		assertEquals(7, m.getDeviceType());
	}

	private static String formatAsUtc(long time) {
		return formatAsUtc(new Date(time));
	}

	private static String formatAsUtc(Date date) {

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(date) + "Z";
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
	public void testSource() {
		String line = "403OviQuMGCqWrRO9>E6fE700@GO";
		AisBaseStation m = new AisBaseStation(Util.getAisExtractorFactory(), line, "boo", 0);
		assertEquals("boo", m.getSource());
		m.toString();
	}
}
