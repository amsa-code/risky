package au.gov.amsa.ais.message;

import static au.gov.amsa.ais.Util.areEqual;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.HasMmsi;
import au.gov.amsa.ais.Util;

/**
 * Decoder for AIS Aid to Navigation data (message type 21).
 * 
 * @author pxg
 * 
 */
public class AisAidToNavigation implements AisMessage, HasMmsi {

    private static final Integer LONGITUDE_NOT_AVAILABLE = 181 * 600000; // 108600000;
    private static final Integer LATITUDE_NOT_AVAILABLE = 91 * 600000; // 54600000;
    private final AisExtractor extractor;
    private final String source;
    private final int messageId;
    private final int repeatIndicator;
    private final int mmsi;
    private final String name;
    private final int dimensionA;
    private final int dimensionB;
    private final int dimensionC;
    private final int dimensionD;
    private final int typeOfElectronicPositionFixingDevice;
    private final boolean isHighAccuracyPosition;
    private final boolean isVirtualAtoN;
    private final boolean isAtonOff;
    private final boolean isAtonInAssignedMode;
    private final Double longitude;
    private final Double latitude;
    private final int timeSecondsOnly;
    private final String atonStatus;
    private final int atonType;
    private final boolean isUsingRAIM;

    public AisAidToNavigation(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    private AisAidToNavigation(AisExtractorFactory factory, String message, String source,
            int padBits) {
        this.source = source;
        extractor = factory.create(message, 172, padBits);
        messageId = extractor.getValue(0, 6);
        Util.checkMessageId(getMessageId(), AisMessageType.ATON_REPORT);
        repeatIndicator = extractor.getValue(6, 8);
        mmsi = extractor.getValue(8, 38);
        atonType = extractor.getValue(38, 43);
        name = extractor.getString(43, 163);
        isHighAccuracyPosition = areEqual(extractor.getValue(163, 164), 1);
        longitude = extractLongitude(extractor);
        latitude = extractLatitude(extractor);
        dimensionA = extractor.getValue(219, 228);
        dimensionB = extractor.getValue(228, 237);
        dimensionC = extractor.getValue(237, 243);
        dimensionD = extractor.getValue(243, 249);
        typeOfElectronicPositionFixingDevice = extractor.getValue(249, 253);
        timeSecondsOnly = extractor.getValue(253, 259);
        isAtonOff = areEqual(extractor.getValue(259, 260), 1);
        atonStatus = extractor.getString(260, 268);
        isUsingRAIM = Util.areEqual(extractor.getValue(268, 269), 1);
        isVirtualAtoN = areEqual(extractor.getValue(269, 270), 1);
        isAtonInAssignedMode = areEqual(extractor.getValue(270, 271), 1);
    }

    @Override
    public int getMessageId() {
        return messageId;
    }

    public int getRepeatIndicator() {
        return repeatIndicator;
    }

    @Override
    public int getMmsi() {
        return mmsi;
    }

    public boolean isHighAccuracyPosition() {
        return isHighAccuracyPosition;
    }

    public boolean isVirtualAtoN() {
        return isVirtualAtoN;
    }

    public boolean isAtonOff() {
        return isAtonOff;
    }

    public boolean isAtonInAssignedMode() {
        return isAtonInAssignedMode;
    }

    public String getAtonStatus() {
        return atonStatus;
    }

    public String getName() {
        return name;
    }

    public int getAtoNType() {
        return atonType;
    }

    public int getDimensionA() {
        return dimensionA;
    }

    public int getDimensionB() {
        return dimensionB;
    }

    public int getDimensionC() {
        return dimensionC;
    }

    public int getDimensionD() {
        return dimensionD;
    }

    public int getLengthMetres() {
        return getDimensionA() + getDimensionB();
    }

    public int getWidthMetres() {
        return getDimensionC() + getDimensionD();
    }

    public int getTypeOfElectronicPositionFixingDevice() {
        return typeOfElectronicPositionFixingDevice;
    }

    @Override
    public String getSource() {
        return source;
    }

    static Double extractLongitude(AisExtractor extractor) {
        int val = extractor.getSignedValue(164, 192);
        if (val == LONGITUDE_NOT_AVAILABLE) {
            return null;
        } else {
            Util.checkLong(val / 600000.0);
            return val / 600000.0;
        }

    }

    static Double extractLatitude(AisExtractor extractor) {
        int val = extractor.getSignedValue(192, 219);
        if (val == LATITUDE_NOT_AVAILABLE) {
            return null;
        } else {
            Util.checkLat(val / 600000.0);
            return val / 600000.0;
        }
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public int getTimeSecondsOnly() {
        return timeSecondsOnly;
    }

    public boolean isUsingRAIM() {
        return isUsingRAIM;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisAidToNavigation [source=");
        builder.append(source);
        builder.append(", messageId=");
        builder.append(messageId);
        builder.append(", repeatIndicator=");
        builder.append(repeatIndicator);
        builder.append(", mmsi=");
        builder.append(mmsi);
        builder.append(", atonType=");
        builder.append(atonType);
        builder.append(", atonStatus=");
        builder.append(atonStatus);
        builder.append(", name=");
        builder.append(name);
        builder.append(", isHighAccuracyPosition=");
        builder.append(isHighAccuracyPosition);
        builder.append(", longitude=");
        builder.append(longitude);
        builder.append(", latitude=");
        builder.append(latitude);
        builder.append(", dimensionA=");
        builder.append(dimensionA);
        builder.append(", dimensionB=");
        builder.append(dimensionB);
        builder.append(", dimensionC=");
        builder.append(dimensionC);
        builder.append(", dimensionD=");
        builder.append(dimensionD);
        builder.append(", typeOfElectronicPositionFixingDevice=");
        builder.append(typeOfElectronicPositionFixingDevice);
        builder.append(", timeSecondsOnly=");
        builder.append(timeSecondsOnly);
        builder.append(", isAtonOff=");
        builder.append(isAtonOff);
        builder.append(", isUsingRAIM=");
        builder.append(isUsingRAIM);
        builder.append(", isVirtualAtoN=");
        builder.append(isVirtualAtoN);
        builder.append(", isAtonInAssignedMode=");
        builder.append(isAtonInAssignedMode);
        builder.append("]");
        return builder.toString();
    }

}
