package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.Util;

import static au.gov.amsa.ais.AisMessageType.POSITION_GPS;

/**
 * Decoder for AIS message type 27 (AIS Satelitte).
 *
 * @author dcuenot
 *
 */
public class AisPositionGPS implements AisPosition {

    private static final Integer COG_NOT_AVAILABLE = 511;
    private static final Integer SOG_NOT_AVAILABLE = 63;
    private static final Integer LONGITUDE_NOT_AVAILABLE = 181 * 600; // 108600;
    private static final Integer LATITUDE_NOT_AVAILABLE = 91 * 600; // 54600;
    private final AisExtractor extractor;
    private final String source;
    private final int messageId;
    private final int mmsi;
    private final Double longitude;
    private final Double latitude;

    public AisPositionGPS(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }
    
    public AisPositionGPS(String message, int padBits) {
        this(message, null, padBits);
    }

    public AisPositionGPS(AisExtractorFactory factory, String message, String source, int padBits) {
        this.source = source;
        this.extractor = factory.create(message, 96, padBits);
        messageId = extractor.getMessageId();
        Util.checkMessageId(messageId, POSITION_GPS);
        mmsi = extractor.getValue(8, 38);
        longitude = extractLongitude(extractor);
        latitude = extractLatitude(extractor);
    }


    static Double extractCourseOverGround(AisExtractor extractor) {
        try {
            int val = extractor.getValue(85, 94);
            if (val == COG_NOT_AVAILABLE || val >= 360)
                return null;
            else
                return val * 1.0;
        } catch (AisParseException e) {
            return null;
        }
    }

    static Double extractSpeedOverGround(AisExtractor extractor) {
        try {
            int val = extractor.getValue(79, 85);
            if (val == SOG_NOT_AVAILABLE)
                return null;
            else
                return val * 1.0;
        } catch (AisParseException e) {
            return null;
        }
    }

    static Double extractLongitude(AisExtractor extractor) {
        try {
            int val = extractor.getSignedValue(44, 62);
            if (val == LONGITUDE_NOT_AVAILABLE) {
                return null;
            } else {
                Util.checkLong(val / 600.0);
                return val / 600.0;
            }
        } catch (AisParseException e) {
            return null;
        }
    }

    static Double extractLatitude(AisExtractor extractor) {

        try {
            int val = extractor.getSignedValue(62, 79);
            if (val == LATITUDE_NOT_AVAILABLE) {
                return null;
            } else {
                Util.checkLat(val / 600.0);
                return val / 600.0;
            }
        } catch (AisParseException e) {
            return null;
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

    public NavigationalStatus getNavigationalStatus() { return NavigationalStatus.values()[extractor.getValue(40, 44)]; }

    @Override
    public Double getSpeedOverGroundKnots() {
        return extractSpeedOverGround(extractor);
    }

    @Override
    public boolean isHighAccuracyPosition() {
        return Util.areEqual(extractor.getValue(38, 39), 1);
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
        return null;
    }

    @Override
    public int getTimeSecondsOnly() {
        return 0;
    }

    public int getSpare() {
        return extractor.getValue(95, 96);
    }

    @Override
    public boolean isUsingRAIM() {
        return Util.areEqual(extractor.getValue(39, 40), 1);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisPositionGPS [source=");
        builder.append(source);
        builder.append(", messageId=");
        builder.append(messageId);
        builder.append(", repeatIndicator=");
        builder.append(getRepeatIndicator());
        builder.append(", mmsi=");
        builder.append(mmsi);
        builder.append(", navigationalStatus=");
        builder.append(getNavigationalStatus());
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
        builder.append(", spare=");
        builder.append(getSpare());
        builder.append(", isUsingRAIM=");
        builder.append(isUsingRAIM());
        builder.append("]");
        return builder.toString();
    }

}