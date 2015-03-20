package au.gov.amsa.ais.message;

import java.util.Calendar;
import java.util.TimeZone;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisExtractorFactory;
import au.gov.amsa.ais.AisMessageType;
import au.gov.amsa.ais.Util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * Decoder for AIS ship static and voyage related data (message type 5).
 * 
 * @author dxm
 * 
 */
public class AisShipStaticA implements AisShipStatic {

	private final String source;
	private final int messageId;
	private Integer repeatIndicator;
	private final int mmsi;
	private Integer aisVersionIndicator;
	private Integer imo;
	private String callsign;
	private String name;
	private Integer dimensionA;
	private Integer dimensionB;
	private Integer dimensionC;
	private Integer dimensionD;
	private Integer typeOfElectronicPositionFixingDevice;
	private Long expectedTimeOfArrival; // lazy set
	private Long expectedTimeOfArrivalUnprocessed;
	private Double maximumPresentStaticDraughtMetres;
	private String destination;
	private Boolean dataTerminalAvailable;
	private Integer spare;
	private Integer shipType;
	private final AisExtractor extractor;

	public AisShipStaticA(String message, String source) {
		this(Util.getAisExtractorFactory(), message, source);
	}

	public AisShipStaticA(AisExtractorFactory factory, String message,
			String source) {
		this.source = source;
		extractor = factory.create(message, 421, padBits);
		messageId = extractor.getValue(0, 6);
		Util.checkMessageId(getMessageId(), AisMessageType.STATIC_AND_VOYAGE_RELATED_DATA);
		mmsi = extractor.getValue(8, 38);
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

	@Override
	public int getRepeatIndicator() {
		if (repeatIndicator == null)
			repeatIndicator = extractor.getValue(6, 8);
		return repeatIndicator;
	}

	@Override
	public long getMmsi() {
		return mmsi;
	}

	public int getAisVersionIndicator() {
		if (aisVersionIndicator == null)
			aisVersionIndicator = extractor.getValue(38, 40);
		return aisVersionIndicator;
	}

	public Optional<Integer> getImo() {
		if (imo == null)
			imo = extractor.getValue(40, 70);
		if (imo == 0)
			return Optional.absent();
		else
			return Optional.of(imo);
	}

	public String getCallsign() {
		if (callsign == null)
			callsign = extractor.getString(70, 112);
		return callsign;
	}

	@Override
	public String getName() {
		if (name == null)
			name = extractor.getString(112, 232);
		return name;
	}

	@Override
	public int getShipType() {
		if (shipType == null)
			shipType = extractor.getValue(232, 240);
		return shipType;
	}

	@Override
	public Optional<Integer> getDimensionA() {
		if (dimensionA == null)
			dimensionA = extractor.getValue(240, 249);
		if (dimensionA == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionA);
	}

	@Override
	public Optional<Integer> getDimensionB() {
		if (dimensionB == null)
			dimensionB = extractor.getValue(249, 258);
		if (dimensionB == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionB);
	}

	@Override
	public Optional<Integer> getDimensionC() {
		if (dimensionC == null)
			dimensionC = extractor.getValue(258, 264);
		if (dimensionC == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionC);
	}

	@Override
	public Optional<Integer> getDimensionD() {
		if (dimensionD == null)
			dimensionD = extractor.getValue(264, 270);
		if (dimensionD == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionD);
	}

	@Override
	public Optional<Integer> getLengthMetres() {
		Optional<Integer> a = getDimensionA();
		Optional<Integer> b = getDimensionB();
		if (a.isPresent() && b.isPresent())
			return Optional.of(a.get() + b.get());
		else {
			Optional<Integer> c = getDimensionC();
			Optional<Integer> d = getDimensionD();
			if (!a.isPresent() && !c.isPresent() && b.isPresent()
					&& d.isPresent())
				return b;
			else
				return Optional.absent();
		}
	}

	@Override
	public Optional<Integer> getWidthMetres() {
		Optional<Integer> c = getDimensionC();
		Optional<Integer> d = getDimensionD();
		if (c.isPresent() && d.isPresent())
			return Optional.of(c.get() + d.get());
		else {
			Optional<Integer> a = getDimensionA();
			Optional<Integer> b = getDimensionB();
			if (!a.isPresent() && !c.isPresent() && b.isPresent()
					&& d.isPresent())
				return d;
			else
				return Optional.absent();
		}
	}

	public int getTypeOfElectronicPositionFixingDevice() {
		if (typeOfElectronicPositionFixingDevice == null)
			typeOfElectronicPositionFixingDevice = extractor.getValue(270, 274);
		return typeOfElectronicPositionFixingDevice;
	}

	private static long getExpectedTimeOfArrival(int month, int day, int hour,
			int minute) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		int year = cal.get(Calendar.YEAR);
		return getExpectedTimeOfArrival(year, month, day, hour, minute);
	}

	@VisibleForTesting
	static long getExpectedTimeOfArrival(int year, int month, int day,
			int hour, int minute) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.clear();
		cal.set(year, month - 1, day, hour, minute);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public long getExpectedTimeOfArrival() {
		if (expectedTimeOfArrival == null) {
			int month = extractor.getValue(274, 278);
			int day = extractor.getValue(278, 283);
			int hour = extractor.getValue(283, 288);
			int minute = extractor.getValue(288, 294);
			expectedTimeOfArrival = getExpectedTimeOfArrival(month, day, hour, minute);
		}
		return expectedTimeOfArrival;
	}

	public long getExpectedTimeOfArrivalUnprocessed() {
		if (expectedTimeOfArrivalUnprocessed == null)
			expectedTimeOfArrivalUnprocessed = (long) extractor.getValue(274, 294);
		return expectedTimeOfArrivalUnprocessed;
	}

	public double getMaximumPresentStaticDraughtMetres() {
		if (maximumPresentStaticDraughtMetres == null)
			maximumPresentStaticDraughtMetres = (double) extractor.getValue(294, 302);
		return maximumPresentStaticDraughtMetres;
	}

	public String getDestination() {
		if (destination == null)
			destination = extractor.getString(302, 422);
		return destination;
	}

	public boolean getDataTerminalAvailable() {
		if (dataTerminalAvailable == null)
			dataTerminalAvailable = Util.areEqual(extractor.getValue(422, 423), 0);
		return dataTerminalAvailable;
	}

	public int getSpare() {
		if (spare == null)
			spare = extractor.getValue(423, 424);
		return spare;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("AisShipStaticA [source=");
		b.append(source);
		b.append(", messageId=");
		b.append(messageId);
		b.append(", repeatIndicator=");
		b.append(repeatIndicator);
		b.append(", mmsi=");
		b.append(mmsi);
		b.append(", aisVersionIndicator=");
		b.append(aisVersionIndicator);
		b.append(", imo=");
		b.append(imo);
		b.append(", callsign=");
		b.append(callsign);
		b.append(", name=");
		b.append(name);
		b.append(", dimensionA=");
		b.append(dimensionA);
		b.append(", dimensionB=");
		b.append(dimensionB);
		b.append(", dimensionC=");
		b.append(dimensionC);
		b.append(", dimensionD=");
		b.append(dimensionD);
		b.append(", typeOfElectronicPositionFixingDevice=");
		b.append(typeOfElectronicPositionFixingDevice);
		b.append(", expectedTimeOfArrival=");
		b.append(expectedTimeOfArrival);
		b.append(", expectedTimeOfArrivalUnprocessed=");
		b.append(expectedTimeOfArrivalUnprocessed);
		b.append(", maximumPresentStaticDraughtMetres=");
		b.append(maximumPresentStaticDraughtMetres);
		b.append(", destination=");
		b.append(destination);
		b.append(", dataTerminalAvailable=");
		b.append(dataTerminalAvailable);
		b.append(", spare=");
		b.append(spare);
		b.append(", shipType=");
		b.append(shipType);
		b.append(", dimensionB=");
		b.append(dimensionB);
		b.append("]");
		return b.toString();
	}

}
