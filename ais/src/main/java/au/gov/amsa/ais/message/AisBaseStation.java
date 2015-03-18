package au.gov.amsa.ais.message;

import java.util.Calendar;
import java.util.TimeZone;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.HasMmsi;
import au.gov.amsa.ais.Util;

import com.google.common.annotations.VisibleForTesting;

/**
 * An AIS base station message (message id 4).
 * 
 * @author dxm
 * 
 */
public class AisBaseStation implements AisMessage,HasMmsi {
	@VisibleForTesting
	static final int MIN_LENGTH = 168;
	private final AisExtractor extractor;
	private final String source;
	private final int year;
	private final int messageId;
	private final int repeatIndicator;
	private final int mmsi;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int positionAccuracy;
	private final long timestamp;
	private final double latitude;
	private final double longitude;
	private final int deviceType;

	/**
	 * Constructor.
	 * 
	 * @param message
	 */
	public AisBaseStation(String message) {
		this(message, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            encapsulated message taken from nmea line.
	 */
	public AisBaseStation(String message, String source) {
		this(Util.getAisExtractorFactory(), message,
				 source);
	}

	/**
	 * Constructor.
	 * 
	 * @param factory
	 * @param message
	 * @param time
	 * @param source
	 */
	public AisBaseStation(AisExtractorFactory factory, String message,
			 String source) {
		this.source = source;
		this.extractor = factory.create(message, MIN_LENGTH);
		messageId = extractor.getMessageId();
		Util.checkMessageId(messageId, AisMessageType.BASE_STATION_REPORT);
		repeatIndicator = extractor.getValue(6, 8);
		mmsi = extractor.getValue(8, 38);
		year = extractor.getValue(38, 52);
		month = extractor.getValue(52, 56);
		day = extractor.getValue(56, 61);
		hour = extractor.getValue(61, 66);
		minute = extractor.getValue(66, 72);
		second = extractor.getValue(72, 78);
		positionAccuracy = extractor.getValue(78, 79);
		timestamp = calculateTimestamp();
		latitude = extractor.getSignedValue(107, 134) / 600000.0;
		longitude = extractor.getSignedValue(79, 107) / 600000.0;
		deviceType = extractor.getValue(134, 138);
		Util.checkLatLong(getLatitude(), getLongitude());

	}

	@Override
	public int getMessageId() {
		return messageId;
	}


	public int getRepeatIndicator() {
		return repeatIndicator;
	}

	@Override
	public long getMmsi() {
		return mmsi;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public int getHour() {
		return hour;
	}

	public int getMinute() {
		return minute;
	}

	public int getSecond() {
		return second;
	}

	public int getPositionAccuracy() {
		return positionAccuracy;
	}

	private long calculateTimestamp() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public int getDeviceType() {
		return deviceType;
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
		builder.append(year);
		builder.append(", messageId=");
		builder.append(messageId);
		builder.append(", repeatIndicator=");
		builder.append(repeatIndicator);
		builder.append(", mmsi=");
		builder.append(mmsi);
		builder.append(", month=");
		builder.append(month);
		builder.append(", day=");
		builder.append(day);
		builder.append(", hour=");
		builder.append(hour);
		builder.append(", minute=");
		builder.append(minute);
		builder.append(", second=");
		builder.append(second);
		builder.append(", positionAccuracy=");
		builder.append(positionAccuracy);
		builder.append(", timestamp=");
		builder.append(timestamp);
		builder.append(", latitude=");
		builder.append(latitude);
		builder.append(", longitude=");
		builder.append(longitude);
		builder.append(", deviceType=");
		builder.append(deviceType);
		builder.append("]");
		return builder.toString();
	}

}
