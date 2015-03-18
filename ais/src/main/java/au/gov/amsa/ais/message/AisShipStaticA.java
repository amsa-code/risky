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
	private final int repeatIndicator;
	private final int mmsi;
	private final int aisVersionIndicator;
	private final int imo;
	private final String callsign;
	private final String name;
	private final int dimensionA;
	private final int dimensionC;
	private final int dimensionD;
	private final int typeOfElectronicPositionFixingDevice;
	private final long expectedTimeOfArrival;
	private final long expectedTimeOfArrivalUnprocessed;
	private final double maximumPresentStaticDraughtMetres;
	private final String destination;
	private final boolean dataTerminalAvailable;
	private final int spare;
	private final int shipType;
	private final int dimensionB;

	private AisShipStaticA(String source, int messageId, int repeatIndicator, int mmsi,
	        int aisVersionIndicator, int imo, String callsign, String name, int dimensionA,
	        int dimensionC, int dimensionD, int typeOfElectronicPositionFixingDevice,
	        long expectedTimeOfArrival, long expectedTimeOfArrivalUnprocessed,
	        double maximumPresentStaticDraughtMetres, String destination,
	        boolean dataTerminalAvailable, int spare, int shipType, int dimensionB) {
		this.source = source;
		this.messageId = messageId;
		this.repeatIndicator = repeatIndicator;
		this.mmsi = mmsi;
		this.aisVersionIndicator = aisVersionIndicator;
		this.imo = imo;
		this.callsign = callsign;
		this.name = name;
		this.dimensionA = dimensionA;
		this.dimensionC = dimensionC;
		this.dimensionD = dimensionD;
		this.typeOfElectronicPositionFixingDevice = typeOfElectronicPositionFixingDevice;
		this.expectedTimeOfArrival = expectedTimeOfArrival;
		this.expectedTimeOfArrivalUnprocessed = expectedTimeOfArrivalUnprocessed;
		this.maximumPresentStaticDraughtMetres = maximumPresentStaticDraughtMetres;
		this.destination = destination;
		this.dataTerminalAvailable = dataTerminalAvailable;
		this.spare = spare;
		this.shipType = shipType;
		this.dimensionB = dimensionB;
	}

	public AisShipStaticA(String message, String source, int padBits) {
		this(Util.getAisExtractorFactory(), message, source, padBits);
	}

	public AisShipStaticA(AisExtractorFactory factory, String message, String source, int padBits) {
		this.source = source;
		AisExtractor extractor = factory.create(message, 421, padBits);
		messageId = extractor.getValue(0, 6);
		Util.checkMessageId(getMessageId(), AisMessageType.STATIC_AND_VOYAGE_RELATED_DATA);
		repeatIndicator = extractor.getValue(6, 8);
		mmsi = extractor.getValue(8, 38);
		aisVersionIndicator = extractor.getValue(38, 40);
		imo = extractor.getValue(40, 70);
		callsign = extractor.getString(70, 112);
		name = extractor.getString(112, 232);
		shipType = extractor.getValue(232, 240);
		dimensionA = extractor.getValue(240, 249);
		dimensionB = extractor.getValue(249, 258);
		dimensionC = extractor.getValue(258, 264);
		dimensionD = extractor.getValue(264, 270);
		typeOfElectronicPositionFixingDevice = extractor.getValue(270, 274);

		int month = extractor.getValue(274, 278);
		int day = extractor.getValue(278, 283);
		int hour = extractor.getValue(283, 288);
		int minute = extractor.getValue(288, 294);
		expectedTimeOfArrival = getExpectedTimeOfArrival(month, day, hour, minute);
		expectedTimeOfArrivalUnprocessed = extractor.getValue(274, 294);
		maximumPresentStaticDraughtMetres = extractor.getValue(294, 302);
		destination = extractor.getString(302, 422);
		dataTerminalAvailable = Util.areEqual(extractor.getValue(422, 423), 0);
		spare = extractor.getValue(423, 424);
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

	public int getAisVersionIndicator() {
		return aisVersionIndicator;
	}

	public Optional<Integer> getImo() {
		if (imo == 0)
			return Optional.absent();
		else
			return Optional.of(imo);
	}

	public String getCallsign() {
		return callsign;
	}

	public String getName() {
		return name;
	}

	public int getShipType() {
		return shipType;
	}

	public Optional<Integer> getDimensionA() {
		if (dimensionA == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionA);
	}

	public Optional<Integer> getDimensionB() {
		if (dimensionB == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionB);
	}

	public Optional<Integer> getDimensionC() {
		if (dimensionC == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionC);
	}

	public Optional<Integer> getDimensionD() {
		if (dimensionD == 0)
			return Optional.absent();
		else
			return Optional.of(dimensionD);
	}

	public Optional<Integer> getLengthMetres() {
		Optional<Integer> a = getDimensionA();
		Optional<Integer> b = getDimensionB();
		if (a.isPresent() && b.isPresent())
			return Optional.of(a.get() + b.get());
		else {
			Optional<Integer> c = getDimensionC();
			Optional<Integer> d = getDimensionD();
			if (!a.isPresent() && !c.isPresent() && b.isPresent() && d.isPresent())
				return b;
			else
				return Optional.absent();
		}
	}

	public Optional<Integer> getWidthMetres() {
		Optional<Integer> c = getDimensionC();
		Optional<Integer> d = getDimensionD();
		if (c.isPresent() && d.isPresent())
			return Optional.of(c.get() + d.get());
		else {
			Optional<Integer> a = getDimensionA();
			Optional<Integer> b = getDimensionB();
			if (!a.isPresent() && !c.isPresent() && b.isPresent() && d.isPresent())
				return d;
			else
				return Optional.absent();
		}
	}

	public int getTypeOfElectronicPositionFixingDevice() {
		return typeOfElectronicPositionFixingDevice;
	}

	private static long getExpectedTimeOfArrival(int month, int day, int hour, int minute) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		int year = cal.get(Calendar.YEAR);
		return getExpectedTimeOfArrival(year, month, day, hour, minute);
	}

	@VisibleForTesting
	static long getExpectedTimeOfArrival(int year, int month, int day, int hour, int minute) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.clear();
		cal.set(year, month - 1, day, hour, minute);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public long getExpectedTimeOfArrival() {
		return expectedTimeOfArrival;
	}

	public long getExpectedTimeOfArrivalUnprocessed() {
		return expectedTimeOfArrivalUnprocessed;
	}

	public double getMaximumPresentStaticDraughtMetres() {
		return maximumPresentStaticDraughtMetres;
	}

	public String getDestination() {
		return destination;
	}

	public boolean getDataTerminalAvailable() {
		return dataTerminalAvailable;
	}

	public int getSpare() {
		return spare;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AisShipStaticA [source=");
		builder.append(source);
		builder.append(", messageId=");
		builder.append(messageId);
		builder.append(", repeatIndicator=");
		builder.append(repeatIndicator);
		builder.append(", mmsi=");
		builder.append(mmsi);
		builder.append(", aisVersionIndicator=");
		builder.append(aisVersionIndicator);
		builder.append(", imo=");
		builder.append(imo);
		builder.append(", callsign=");
		builder.append(callsign);
		builder.append(", name=");
		builder.append(name);
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
		builder.append(", expectedTimeOfArrival=");
		builder.append(expectedTimeOfArrival);
		builder.append(", expectedTimeOfArrivalUnprocessed=");
		builder.append(expectedTimeOfArrivalUnprocessed);
		builder.append(", maximumPresentStaticDraughtMetres=");
		builder.append(maximumPresentStaticDraughtMetres);
		builder.append(", destination=");
		builder.append(destination);
		builder.append(", dataTerminalAvailable=");
		builder.append(dataTerminalAvailable);
		builder.append(", spare=");
		builder.append(spare);
		builder.append(", shipType=");
		builder.append(shipType);
		builder.append(", dimensionB=");
		builder.append(dimensionB);
		builder.append("]");
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String source;
		private int messageId;
		private int repeatIndicator;
		private int mmsi;
		private int aisVersionIndicator;
		private int imo;
		private String callsign;
		private String name;
		private int dimensionA;
		private int dimensionC;
		private int dimensionD;
		private int typeOfElectronicPositionFixingDevice;
		private long expectedTimeOfArrival;
		private long expectedTimeOfArrivalUnprocessed;
		private double maximumPresentStaticDraughtMetres;
		private String destination;
		private boolean dataTerminalAvailable;
		private int spare;
		private int shipType;
		private int dimensionB;

		private Builder() {
		}

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder messageId(int messageId) {
			this.messageId = messageId;
			return this;
		}

		public Builder repeatIndicator(int repeatIndicator) {
			this.repeatIndicator = repeatIndicator;
			return this;
		}

		public Builder mmsi(int mmsi) {
			this.mmsi = mmsi;
			return this;
		}

		public Builder aisVersionIndicator(int aisVersionIndicator) {
			this.aisVersionIndicator = aisVersionIndicator;
			return this;
		}

		public Builder imo(int imo) {
			this.imo = imo;
			return this;
		}

		public Builder callsign(String callsign) {
			this.callsign = callsign;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder dimensionA(int dimensionA) {
			this.dimensionA = dimensionA;
			return this;
		}

		public Builder dimensionC(int dimensionC) {
			this.dimensionC = dimensionC;
			return this;
		}

		public Builder dimensionD(int dimensionD) {
			this.dimensionD = dimensionD;
			return this;
		}

		public Builder typeOfElectronicPositionFixingDevice(int typeOfElectronicPositionFixingDevice) {
			this.typeOfElectronicPositionFixingDevice = typeOfElectronicPositionFixingDevice;
			return this;
		}

		public Builder expectedTimeOfArrival(long expectedTimeOfArrival) {
			this.expectedTimeOfArrival = expectedTimeOfArrival;
			return this;
		}

		public Builder expectedTimeOfArrivalUnprocessed(long expectedTimeOfArrivalUnprocessed) {
			this.expectedTimeOfArrivalUnprocessed = expectedTimeOfArrivalUnprocessed;
			return this;
		}

		public Builder maximumPresentStaticDraughtMetres(double maximumPresentStaticDraughtMetres) {
			this.maximumPresentStaticDraughtMetres = maximumPresentStaticDraughtMetres;
			return this;
		}

		public Builder destination(String destination) {
			this.destination = destination;
			return this;
		}

		public Builder dataTerminalAvailable(boolean dataTerminalAvailable) {
			this.dataTerminalAvailable = dataTerminalAvailable;
			return this;
		}

		public Builder spare(int spare) {
			this.spare = spare;
			return this;
		}

		public Builder shipType(int shipType) {
			this.shipType = shipType;
			return this;
		}

		public Builder dimensionB(int dimensionB) {
			this.dimensionB = dimensionB;
			return this;
		}

		public AisShipStaticA build() {
			return new AisShipStaticA(source, messageId, repeatIndicator, mmsi,
			        aisVersionIndicator, imo, callsign, name, dimensionA, dimensionC, dimensionD,
			        typeOfElectronicPositionFixingDevice, expectedTimeOfArrival,
			        expectedTimeOfArrivalUnprocessed, maximumPresentStaticDraughtMetres,
			        destination, dataTerminalAvailable, spare, shipType, dimensionB);
		}
	}

}
