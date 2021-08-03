package au.gov.amsa.ais.message;

import java.util.Calendar;
import java.util.TimeZone;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.HasMmsi;
import au.gov.amsa.ais.Util;

/**
 * An AIS base station message (message id 4).
 * 
 * @author dxm
 * 
 */
public class AisBaseStation implements AisMessage, HasMmsi {
    @VisibleForTesting
    static final int MIN_LENGTH = 168;
    private final AisExtractor extractor;
    private final String source;
    private final int messageId;

    /**
     * Constructor.
     * 
     * @param message
     */
    public AisBaseStation(String message, int padBits) {
        this(message, null, padBits);
    }

    /**
     * Constructor.
     * 
     * @param message
     *            encapsulated message taken from nmea line.
     */
    public AisBaseStation(String message, String source, int padBits) {
        this(Util.getAisExtractorFactory(), message, source, padBits);
    }

    /**
     * Constructor.
     * 
     * @param factory
     * @param message
     * @param time
     * @param source
     */
    public AisBaseStation(AisExtractorFactory factory, String message, String source, int padBits) {
        this.source = source;
        this.extractor = factory.create(message, MIN_LENGTH, padBits);
        messageId = extractor.getMessageId();
        Util.checkMessageId(messageId, AisMessageType.BASE_STATION_REPORT);
        Util.checkLatLong(getLatitude(), getLongitude());

    }

    @Override
    public int getMessageId() {
        return messageId;
    }

    public int getRepeatIndicator() {
        return extractor.getValue(6, 8);
    }

    @Override
    public int getMmsi() {
        return extractor.getValue(8, 38);
    }

    public int getYear() {
        return extractor.getValue(38, 52);
    }

    public int getMonth() {
        return extractor.getValue(52, 56);
    }

    public int getDay() {
        return extractor.getValue(56, 61);
    }

    public int getHour() {
        return extractor.getValue(61, 66);
    }

    public int getMinute() {
        return extractor.getValue(66, 72);
    }

    public int getSecond() {
        return extractor.getValue(72, 78);
    }

    public int getPositionAccuracy() {
        return extractor.getValue(78, 79);
    }

    private long calculateTimestamp() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(getYear(), getMonth() - 1, getDay(), getHour(), getMinute(), getSecond());
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public long getTimestamp() {
        return calculateTimestamp();
    }

    public double getLatitude() {
        return extractor.getSignedValue(107, 134) / 600000.0;
    }

    public double getLongitude() {
        return extractor.getSignedValue(79, 107) / 600000.0;
    }

    public int getDeviceType() {
        return extractor.getValue(134, 138);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AisBaseStation [source=");
        builder.append(source);
        builder.append(", year=");
        builder.append(getYear());
        builder.append(", messageId=");
        builder.append(messageId);
        builder.append(", repeatIndicator=");
        builder.append(getRepeatIndicator());
        builder.append(", mmsi=");
        builder.append(getMmsi());
        builder.append(", month=");
        builder.append(getMonth());
        builder.append(", day=");
        builder.append(getDay());
        builder.append(", hour=");
        builder.append(getHour());
        builder.append(", minute=");
        builder.append(getMinute());
        builder.append(", second=");
        builder.append(getSecond());
        builder.append(", positionAccuracy=");
        builder.append(getPositionAccuracy());
        builder.append(", timestamp=");
        builder.append(getTimestamp());
        builder.append(", latitude=");
        builder.append(getLatitude());
        builder.append(", longitude=");
        builder.append(getLongitude());
        builder.append(", deviceType=");
        builder.append(getDeviceType());
        builder.append("]");
        return builder.toString();
    }

}
