package au.gov.amsa.ais.message;

import static au.gov.amsa.ais.Util.areEqual;
import static au.gov.amsa.ais.Util.checkMessageId;
import static au.gov.amsa.ais.Util.getAisExtractorFactory;
import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.Communications;
import au.gov.amsa.ais.HasCommunications;
import au.gov.amsa.ais.Util;

public class AisPositionB implements AisPosition, HasCommunications {

	private static final Integer TRUE_HEADING_NOT_AVAILABLE = 511;
	private static final Integer COG_NOT_AVAILABLE = 3600;
	private static final Integer SOG_NOT_AVAILABLE = 1023;
	private static final Integer LONGITUDE_NOT_AVAILABLE = 181 * 600000; // 108600000;
	private static final Integer LATITUDE_NOT_AVAILABLE = 91 * 600000; // 54600000;
	private final AisExtractor extractor;
	private final String source;
	private final int messageId;
	private final int repeatIndicator;
	private final int mmsi;
	private final int spare;
	private final Double speedOverGroundKnots;
	private final boolean isHighAccuracyPosition;
	private final Double longitude;
	private final Double latitude;
	private final Double courseOverGround;
	private final Integer trueHeading;
	private final int timeSecondsOnly;
	private final int spare2;
	private final boolean isSotdmaUnit;
	private final boolean isEquippedWithIntegratedDisplayForMessages12And14;
	private final boolean isEquippedWithDscFunction;
	private final boolean canOperateOverWholeMarineBand;
	private final boolean canManageFrequenciesViaMessage22;
	private final boolean isStationOperatingInAssignedMode;
	private final boolean isUsingRAIM;
	private final boolean isITDMACommunicationState;
	private final Communications communications;

	public AisPositionB(String message, String source) {
		this(getAisExtractorFactory(), message, source);
	}

	public AisPositionB(String message) {
		this(getAisExtractorFactory(), message, null);
	}

	public AisPositionB(AisExtractorFactory factory, String message,
			String source) {
		this.source = source;
		this.extractor = factory.create(message, 133);
		messageId = extractor.getMessageId();
		checkMessageId(getMessageId(), AisMessageType.POSITION_REPORT_CLASS_B);
		repeatIndicator = extractor.getValue(6, 8);
		mmsi = extractor.getValue(8, 38);
		spare = extractor.getValue(38, 46);
		speedOverGroundKnots = extractSpeedOverGround(extractor);
		isHighAccuracyPosition = areEqual(extractor.getValue(56, 57), 1);
		longitude = extractLongitude(extractor);
		latitude = extractLatitude(extractor);
		courseOverGround = extractCourseOverGround(extractor);
		trueHeading = extractTrueHeading(extractor);
		timeSecondsOnly = extractor.getValue(133, 139);
		spare2 = extractor.getValue(139, 141);
		isSotdmaUnit = areEqual(extractor.getValue(141, 142), 0);
		isEquippedWithIntegratedDisplayForMessages12And14 = areEqual(
				extractor.getValue(142, 143), 1);
		isEquippedWithDscFunction = areEqual(extractor.getValue(143, 144), 1);
		canOperateOverWholeMarineBand = areEqual(extractor.getValue(144, 145),
				1);
		canManageFrequenciesViaMessage22 = areEqual(
				extractor.getValue(145, 146), 1);
		isStationOperatingInAssignedMode = areEqual(
				extractor.getValue(146, 147), 1);
		isUsingRAIM = areEqual(extractor.getValue(147, 148), 1);
		isITDMACommunicationState = areEqual(extractor.getValue(148, 149), 1);
		communications = new Communications(extractor, 149);
	}

	static Integer extractTrueHeading(AisExtractor extractor) {
		int val = extractor.getValue(124, 133);
		if (val == TRUE_HEADING_NOT_AVAILABLE)
			return null;
		else if (val > 359) {
			// have seen 404, might be corrupted message
			return null;
		} else
			return val;
	}

	static Double extractCourseOverGround(AisExtractor extractor) {
		int val = extractor.getValue(112, 124);
		if (val == COG_NOT_AVAILABLE || val >= 3600)
			return null;
		else
			return val / 10.0;
	}

	static Double extractSpeedOverGround(AisExtractor extractor) {
		int val = extractor.getValue(46, 56);
		if (val == SOG_NOT_AVAILABLE)
			return null;
		else
			return val / 10.0;
	}

	static Double extractLongitude(AisExtractor extractor) {
		int val = extractor.getSignedValue(57, 85);
		if (val == LONGITUDE_NOT_AVAILABLE) {
			return null;
		} else {
			Util.checkLong(val / 600000.0);
			return val / 600000.0;
		}
	}

	static Double extractLatitude(AisExtractor extractor) {
		int val = extractor.getSignedValue(85, 112);
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
		return repeatIndicator;
	}

	@Override
	public long getMmsi() {
		return mmsi;
	}

	public int getSpare() {
		return spare;
	}

	@Override
	public Double getSpeedOverGroundKnots() {
		return speedOverGroundKnots;
	}

	@Override
	public boolean isHighAccuracyPosition() {
		return isHighAccuracyPosition;
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
		return courseOverGround;
	}

	@Override
	public Integer getTrueHeading() {
		return trueHeading;
	}

	@Override
	public int getTimeSecondsOnly() {
		return timeSecondsOnly;
	}

	public int getSpare2() {
		return spare2;
	}

	public boolean isSotdmaUnit() {
		return isSotdmaUnit;
	}

	public boolean isEquippedWithIntegratedDisplayForMessages12And14() {
		return isEquippedWithIntegratedDisplayForMessages12And14;
	}

	public boolean isEquippedWithDscFunction() {
		return isEquippedWithDscFunction;
	}

	public boolean canOperateOverWholeMarineBand() {
		return canOperateOverWholeMarineBand;
	}

	public boolean canManageFrequenciesViaMessage22() {
		return canManageFrequenciesViaMessage22;
	}

	public boolean isStationOperatingInAssignedMode() {
		return isStationOperatingInAssignedMode;
	}

	@Override
	public boolean isUsingRAIM() {
		return isUsingRAIM;
	}

	public boolean isITDMACommunicationState() {
		return isITDMACommunicationState;
	}

	@Override
	public Communications getCommunications() {
		return communications;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AisPositionB [source=");
		builder.append(source);
		builder.append(", messageId=");
		builder.append(messageId);
		builder.append(", repeatIndicator=");
		builder.append(repeatIndicator);
		builder.append(", mmsi=");
		builder.append(mmsi);
		builder.append(", spare=");
		builder.append(spare);
		builder.append(", speedOverGroundKnots=");
		builder.append(speedOverGroundKnots);
		builder.append(", isHighAccuracyPosition=");
		builder.append(isHighAccuracyPosition);
		builder.append(", longitude=");
		builder.append(longitude);
		builder.append(", latitude=");
		builder.append(latitude);
		builder.append(", courseOverGround=");
		builder.append(courseOverGround);
		builder.append(", trueHeading=");
		builder.append(trueHeading);
		builder.append(", timeSecondsOnly=");
		builder.append(timeSecondsOnly);
		builder.append(", spare2=");
		builder.append(spare2);
		builder.append(", isSotdmaUnit=");
		builder.append(isSotdmaUnit);
		builder.append(", isEquippedWithIntegratedDisplayForMessages12And14=");
		builder.append(isEquippedWithIntegratedDisplayForMessages12And14);
		builder.append(", isEquippedWithDscFunction=");
		builder.append(isEquippedWithDscFunction);
		builder.append(", canOperateOverWholeMarineBand=");
		builder.append(canOperateOverWholeMarineBand);
		builder.append(", canManageFrequenciesViaMessage22=");
		builder.append(canManageFrequenciesViaMessage22);
		builder.append(", isStationOperatingInAssignedMode=");
		builder.append(isStationOperatingInAssignedMode);
		builder.append(", isUsingRAIM=");
		builder.append(isUsingRAIM);
		builder.append(", isITDMACommunicationState=");
		builder.append(isITDMACommunicationState);
		builder.append(", communications=");
		builder.append(communications);
		builder.append("]");
		return builder.toString();
	}

}
