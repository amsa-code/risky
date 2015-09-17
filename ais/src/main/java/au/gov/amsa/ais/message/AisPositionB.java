package au.gov.amsa.ais.message;

import static au.gov.amsa.ais.Util.areEqual;
import static au.gov.amsa.ais.Util.checkMessageId;
import static au.gov.amsa.ais.Util.getAisExtractorFactory;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.AisParseException;
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
    private final int mmsi;
    private final Double longitude;
    private final Double latitude;

    public AisPositionB(String message, String source, int padBits) {
        this(getAisExtractorFactory(), message, source, padBits);
    }

    public AisPositionB(String message, int padBits) {
        this(getAisExtractorFactory(), message, null, padBits);
    }

    public AisPositionB(AisExtractorFactory factory, String message, String source, int padBits) {
        this.source = source;
        this.extractor = factory.create(message, 133, padBits);
        messageId = extractor.getMessageId();
        checkMessageId(getMessageId(), AisMessageType.POSITION_REPORT_CLASS_B);

        mmsi = extractor.getValue(8, 38);
        longitude = extractLongitude(extractor);
        latitude = extractLatitude(extractor);
    }

    static Integer extractTrueHeading(AisExtractor extractor) {
        try {
            int val = extractor.getValue(124, 133);
            if (val == TRUE_HEADING_NOT_AVAILABLE)
                return null;
            else if (val > 359) {
                // have seen 404, might be corrupted message
                return null;
            } else
                return val;
        } catch (AisParseException e) {
            return null;
        }
    }

    static Double extractCourseOverGround(AisExtractor extractor) {
        try {
            int val = extractor.getValue(112, 124);
            if (val == COG_NOT_AVAILABLE || val >= 3600)
                return null;
            else
                return val / 10.0;
        } catch (AisParseException e) {
            return null;
        }
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
        return extractor.getValue(6, 8);
    }

    @Override
    public int getMmsi() {
        return mmsi;
    }

    public int getSpare() {
        return extractor.getValue(38, 46);
    }

    @Override
    public Double getSpeedOverGroundKnots() {
        return extractSpeedOverGround(extractor);
    }

    @Override
    public boolean isHighAccuracyPosition() {
        return areEqual(extractor.getValue(56, 57), 1);
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
        return extractor.getValue(133, 139);
    }

    public int getSpare2() {
        return extractor.getValue(139, 141);
    }

    public boolean isSotdmaUnit() {
        return areEqual(extractor.getValue(141, 142), 0);
    }

    public boolean isEquippedWithIntegratedDisplayForMessages12And14() {
        return areEqual(extractor.getValue(142, 143), 1);
    }

    public boolean isEquippedWithDscFunction() {
        return areEqual(extractor.getValue(143, 144), 1);
    }

    public boolean canOperateOverWholeMarineBand() {
        return areEqual(extractor.getValue(144, 145), 1);
    }

    public boolean canManageFrequenciesViaMessage22() {
        return areEqual(extractor.getValue(145, 146), 1);
    }

    public boolean isStationOperatingInAssignedMode() {
        return areEqual(extractor.getValue(146, 147), 1);
    }

    @Override
    public boolean isUsingRAIM() {
        return areEqual(extractor.getValue(147, 148), 1);
    }

    public boolean isITDMACommunicationState() {
        return areEqual(extractor.getValue(148, 149), 1);
    }

    @Override
    public Communications getCommunications() {
        return new Communications(extractor, 149);
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
        builder.append(getRepeatIndicator());
        builder.append(", mmsi=");
        builder.append(mmsi);
        builder.append(", spare=");
        builder.append(getSpare());
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
        builder.append(", spare2=");
        builder.append(getSpare2());
        builder.append(", isSotdmaUnit=");
        builder.append(isSotdmaUnit());
        builder.append(", isEquippedWithIntegratedDisplayForMessages12And14=");
        builder.append(isEquippedWithIntegratedDisplayForMessages12And14());
        builder.append(", isEquippedWithDscFunction=");
        builder.append(isEquippedWithDscFunction());
        builder.append(", canOperateOverWholeMarineBand=");
        builder.append(canOperateOverWholeMarineBand());
        builder.append(", canManageFrequenciesViaMessage22=");
        builder.append(canManageFrequenciesViaMessage22());
        builder.append(", isStationOperatingInAssignedMode=");
        builder.append(isStationOperatingInAssignedMode());
        builder.append(", isUsingRAIM=");
        builder.append(isUsingRAIM());
        builder.append(", isITDMACommunicationState=");
        builder.append(isITDMACommunicationState());
        builder.append(", communications=");
        builder.append(getCommunications());
        builder.append("]");
        return builder.toString();
    }

}
