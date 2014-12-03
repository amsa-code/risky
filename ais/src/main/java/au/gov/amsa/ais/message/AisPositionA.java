package au.gov.amsa.ais.message;

import static au.gov.amsa.ais.AisMessageType.POSITION_REPORT_ASSIGNED;
import static au.gov.amsa.ais.AisMessageType.POSITION_REPORT_SCHEDULED;
import static au.gov.amsa.ais.AisMessageType.POSITION_REPORT_SPECIAL;
import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.Communications;
import au.gov.amsa.ais.HasCommunications;
import au.gov.amsa.ais.Util;

/**
 * Decoder for AIS message types 1, 2,3 (Class A position reports).
 * 
 * @author dxm
 * 
 */
public class AisPositionA implements AisPosition, HasCommunications {

	private static final Integer TRUE_HEADING_NOT_AVAILABLE = 511;
	private static final Integer COG_NOT_AVAILABLE = 3600;
	private static final Integer SOG_NOT_AVAILABLE = 1023;
	private static final Integer ROT_NOT_AVAILABLE = -128;
	private static final Integer LONGITUDE_NOT_AVAILABLE = 181 * 600000; // 108600000;
	private static final Integer LATITUDE_NOT_AVAILABLE = 91 * 600000; // 54600000;
	private final AisExtractor extractor;
	private final String source;
	private final int messageId;
	private final int mmsi;
	private final Double longitude;
	private final Double latitude;

	public AisPositionA(String message, String source) {
		this(Util.getAisExtractorFactory(), message, source);
	}

	public AisPositionA(String message) {
		this(Util.getAisExtractorFactory(), message, null);
	}

	public AisPositionA(AisExtractorFactory factory, String message,
			String source) {
		this.source = source;
		this.extractor = factory.create(message, 137);
		messageId = extractor.getMessageId();
		Util.checkMessageId(messageId, POSITION_REPORT_SCHEDULED,
				POSITION_REPORT_ASSIGNED, POSITION_REPORT_SPECIAL);
		mmsi = extractor.getValue(8, 38);
		longitude = extractLongitude(extractor);
		latitude = extractLatitude(extractor);
	}

	static Integer extractTrueHeading(AisExtractor extractor) {
		int val = extractor.getValue(128, 137);
		if (val == TRUE_HEADING_NOT_AVAILABLE)
			return null;
		else if (val > 359) {
			// have seen 404, might be corrupted message
			return null;
		} else
			return val;
	}

	static Double extractCourseOverGround(AisExtractor extractor) {
		int val = extractor.getValue(116, 128);
		if (val == COG_NOT_AVAILABLE || val >= 3600)
			return null;
		else
			return val / 10.0;
	}

	static Double extractSpeedOverGround(AisExtractor extractor) {
		int val = extractor.getValue(50, 60);
		if (val == SOG_NOT_AVAILABLE)
			return null;
		else
			return val / 10.0;
	}

	static Integer extractRateOfTurn(AisExtractor extractor) {
		int val = extractor.getSignedValue(42, 50);
		if (val == ROT_NOT_AVAILABLE)
			return null;
		else
			return val;
	}

	static Double extractLongitude(AisExtractor extractor) {
		int val = extractor.getSignedValue(61, 89);
		if (val == LONGITUDE_NOT_AVAILABLE) {
			return null;
		} else {
			Util.checkLong(val / 600000.0);
			return val / 600000.0;
		}

	}

	static Double extractLatitude(AisExtractor extractor) {
		int val = extractor.getSignedValue(89, 116);
		if (val == LATITUDE_NOT_AVAILABLE) {
			return null;
		} else {
			Util.checkLat(val / 600000.0);
			return val / 600000.0;
		}
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

	@Override
	public int getRepeatIndicator() {
		return extractor.getValue(6, 8);
	}

	@Override
	public long getMmsi() {
		return mmsi;
	}

	public int getNavigationalStatus() {
		return extractor.getValue(38, 42);
	}

	public Integer getRateOfTurn() {
		return extractRateOfTurn(extractor);
	}

	@Override
	public Double getSpeedOverGroundKnots() {
		return extractSpeedOverGround(extractor);
	}

	@Override
	public boolean isHighAccuracyPosition() {
		return Util.areEqual(extractor.getValue(60, 61), 1);
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	@Override
	public Double getLatitude() {
		return latitude;
	}

	@Override
	public Double getCourseOverGround() {
		return extractCourseOverGround(extractor);
	}

	@Override
	public Integer getTrueHeading() {
		return extractTrueHeading(extractor);
	}

	@Override
	public int getTimeSecondsOnly() {
		return extractor.getValue(137, 143);
	}

	public int getSpecialManoeuvreIndicator() {
		return extractor.getValue(143, 145);
	}

	public int getSpare() {
		return extractor.getValue(145, 148);
	}

	@Override
	public boolean isUsingRAIM() {
		return Util.areEqual(extractor.getValue(148, 149), 1);
	}

	@Override
	public Communications getCommunications() {
		return new Communications(extractor, 149);
	}

	public boolean isAtAnchor() {
		return getNavigationalStatus() == 1;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AisPositionA [source=");
		builder.append(source);
		builder.append(", messageId=");
		builder.append(messageId);
		builder.append(", repeatIndicator=");
		builder.append(getRepeatIndicator());
		builder.append(", mmsi=");
		builder.append(mmsi);
		builder.append(", navigationalStatus=");
		builder.append(getNavigationalStatus());
		builder.append(", rateOfTurn=");
		builder.append(getRateOfTurn());
		builder.append(", speedOverGroundKnots=");
		builder.append(getSpeedOverGroundKnots());
		builder.append(", isHighAccuracyPosition=");
		builder.append(isHighAccuracyPosition());
		builder.append(", longitude=");
		builder.append(longitude);
		builder.append(", latitude=");
		builder.append(latitude);
		builder.append(", courseOverGround=");
		builder.append(getCourseOverGround());
		builder.append(", trueHeading=");
		builder.append(getTrueHeading());
		builder.append(", timeSecondsOnly=");
		builder.append(getTimeSecondsOnly());
		builder.append(", specialManoeuvreIndicator=");
		builder.append(getSpecialManoeuvreIndicator());
		builder.append(", spare=");
		builder.append(getSpare());
		builder.append(", isUsingRAIM=");
		builder.append(isUsingRAIM());
		builder.append(", communications=");
		builder.append(getCommunications());
		builder.append("]");
		return builder.toString();
	}

}
