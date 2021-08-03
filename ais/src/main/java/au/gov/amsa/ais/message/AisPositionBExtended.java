package au.gov.amsa.ais.message;

import java.util.Optional;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.Util;

public class AisPositionBExtended implements AisPosition, AisShipStatic {

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
    private final Double speedOverGroundKnots;
    private final Double courseOverGround;
    private final Double latitude;
    private final Double longitude;
    private final Integer trueHeading;
    private final String name;
    private final int shipType;
    private final int dimensionA;
    private final int dimensionB;
    private final int dimensionC;
    private final int dimensionD;
    private final int timeSecondsOnly;
    private final boolean isHighAccuracyPosition;
    private final boolean isUsingRAIM;

    public AisPositionBExtended(String message, int padBits) {
        this(message, null, padBits);
    }

    public AisPositionBExtended(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    public AisPositionBExtended(AisExtractorFactory factory, String message, String source,
            int padBits) {
        this.source = source;
        this.extractor = factory.create(message, 301, padBits);
        messageId = extractor.getMessageId();
        Util.checkMessageId(getMessageId(), AisMessageType.POSITION_REPORT_CLASS_B_EXTENDED);
        repeatIndicator = extractor.getValue(6, 8);
        mmsi = extractor.getValue(8, 38);
        speedOverGroundKnots = extractSpeedOverGround(extractor);
        isHighAccuracyPosition = Util.areEqual(extractor.getValue(56, 57), 1);
        longitude = extractLongitude(extractor);
        latitude = extractLatitude(extractor);
        courseOverGround = extractCourseOverGround(extractor);
        trueHeading = extractTrueHeading(extractor);
        timeSecondsOnly = extractor.getValue(133, 139);
        name = extractor.getString(143, 263);
        shipType = extractor.getValue(263, 271);
        dimensionA = extractor.getValue(271, 280);
        dimensionB = extractor.getValue(280, 289);
        dimensionC = extractor.getValue(289, 295);
        dimensionD = extractor.getValue(295, 301);
        isUsingRAIM = Util.areEqual(extractor.getValue(305, 306), 1);
    }

    static Integer extractTrueHeading(AisExtractor extractor) {
        int val = extractor.getValue(124, 133);
        if (val == TRUE_HEADING_NOT_AVAILABLE)
            return null;
        else
            return val;
    }

    static Double extractCourseOverGround(AisExtractor extractor) {
        int val = extractor.getValue(112, 124);
        if (val == COG_NOT_AVAILABLE)
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
    public int getMmsi() {
        return mmsi;
    }

    @Override
    public Double getSpeedOverGroundKnots() {
        return speedOverGroundKnots;
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

    @Override
    public String getName() {
        return name;

    }

    @Override
    public int getShipType() {
        return shipType;
    }

    @Override
    public Optional<Integer> getDimensionA() {
        if (dimensionA == 0)
            return Optional.empty();
        else
            return Optional.of(dimensionA);
    }

    @Override
    public Optional<Integer> getDimensionB() {
        if (dimensionB == 0)
            return Optional.empty();
        else
            return Optional.of(dimensionB);
    }

    @Override
    public Optional<Integer> getDimensionC() {
        if (dimensionC == 0)
            return Optional.empty();
        else
            return Optional.of(dimensionC);
    }

    @Override
    public Optional<Integer> getDimensionD() {
        if (dimensionD == 0)
            return Optional.empty();
        else
            return Optional.of(dimensionD);
    }

    @Override
    public Optional<Integer> getLengthMetres() {
        Optional<Integer> a = getDimensionA();
        if (!a.isPresent())
            return Optional.empty();
        Optional<Integer> b = getDimensionB();
        if (!b.isPresent())
            return Optional.empty();
        return Optional.of(a.get() + b.get());
    }

    @Override
    public Optional<Integer> getWidthMetres() {
        Optional<Integer> c = getDimensionC();
        if (!c.isPresent())
            return Optional.empty();
        Optional<Integer> d = getDimensionD();
        if (!d.isPresent())
            return Optional.empty();
        return Optional.of(c.get() + d.get());
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public boolean isHighAccuracyPosition() {
        return isHighAccuracyPosition;
    }

    @Override
    public boolean isUsingRAIM() {
        return isUsingRAIM;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisPositionBExtended [source=");
        builder.append(source);
        builder.append(", messageId=");
        builder.append(messageId);
        builder.append(", mmsi=");
        builder.append(mmsi);
        builder.append(", repeatIndicator=");
        builder.append(getRepeatIndicator());
        builder.append(", speedOverGroundKnots=");
        builder.append(getSpeedOverGroundKnots());
        builder.append(", isHighAccuracyPosition=");
        builder.append(isHighAccuracyPosition());
        builder.append(", courseOverGround=");
        builder.append(getCourseOverGround());
        builder.append(", latitude=");
        builder.append(getLatitude());
        builder.append(", longitude=");
        builder.append(getLongitude());
        builder.append(", trueHeading=");
        builder.append(getTrueHeading());
        builder.append(", name=");
        builder.append(getName());
        builder.append(", shipType=");
        builder.append(getShipType());
        builder.append(", dimensionA=");
        builder.append(getDimensionA());
        builder.append(", dimensionB=");
        builder.append(getDimensionB());
        builder.append(", dimensionC=");
        builder.append(getDimensionC());
        builder.append(", dimensionD=");
        builder.append(getDimensionD());
        builder.append(", timeSecondsOnly=");
        builder.append(getTimeSecondsOnly());
        builder.append("]");
        return builder.toString();
    }

}
