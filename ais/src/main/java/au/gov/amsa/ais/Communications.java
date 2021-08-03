package au.gov.amsa.ais;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

/**
 * Provides the fields inside the Communications portion of an AIS Message.
 * 
 * @author dxm
 * 
 */
public class Communications {

	private final int startIndex;
	private final int syncState;
	private final int slotTimeout;
	private final Integer receivedStations;
	private final Integer slotNumber;
	private final Integer hourUtc;
	private final Integer minuteUtc;
	private final Integer slotOffset;

	/**
	 * Constructor.
	 * 
	 * @param extractor
	 * @param startIndex
	 */
	public Communications(AisExtractor extractor, int startIndex) {
		this.startIndex = startIndex;
		syncState = extractor.getValue(startIndex, startIndex + 2);
		slotTimeout = extractor.getValue(startIndex + 2, startIndex + 5);
		receivedStations = getReceivedStations(extractor, slotTimeout,
				startIndex);
		slotNumber = getSlotNumber(extractor, slotTimeout, startIndex);
		hourUtc = getHourUtc(extractor, slotTimeout, startIndex);
		minuteUtc = getMinuteUtc(extractor, slotTimeout, startIndex);
		slotOffset = getSlotOffset(extractor, slotTimeout, startIndex);
	}

	/**
	 * Returns received stations as per 1371-4.pdf.
	 * 
	 * @param extractor
	 * @param slotTimeout
	 * @param startIndex
	 * @return
	 */
	@VisibleForTesting
	static Integer getReceivedStations(AisExtractor extractor, int slotTimeout,
			int startIndex) {
		if (slotTimeout == 3 || slotTimeout == 5 || slotTimeout == 7)
			return extractor.getValue(startIndex + 5, startIndex + 19);
		else
			return null;
	}

	/**
	 * Returns slot number as per 1371-4.pdf.
	 * 
	 * @param extractor
	 * @param slotTimeout
	 * @param startIndex
	 * @return
	 */
	@VisibleForTesting
	static Integer getSlotNumber(AisExtractor extractor, int slotTimeout,
			int startIndex) {
		if (slotTimeout == 2 || slotTimeout == 4 || slotTimeout == 6)
			return extractor.getValue(startIndex + 5, startIndex + 19);
		else
			return null;
	}

	/**
	 * Returns hour UTC as per 1371-4.pdf.
	 * 
	 * @param extractor
	 * @param slotTimeout
	 * @param startIndex
	 * @return
	 */
	private static Integer getHourUtc(AisExtractor extractor, int slotTimeout,
			int startIndex) {
		if (slotTimeout == 1) {
			// skip the msb bit
			int hours = extractor.getValue(startIndex + 5, startIndex + 10);
			return hours;
		} else
			return null;
	}

	/**
	 * Returns minute UTC as per 1371-4.pdf.
	 * 
	 * @param extractor
	 * @param slotTimeout
	 * @param startIndex
	 * @return
	 */
	private static Integer getMinuteUtc(AisExtractor extractor,
			int slotTimeout, int startIndex) {
		if (slotTimeout == 1) {
			// skip the msb bit
			int minutes = extractor.getValue(startIndex + 10, startIndex + 17);
			return minutes;
		} else
			return null;
	}

	/**
	 * Returns slot offset as per 1371-4.pdf.
	 * 
	 * @param extractor
	 * @param slotTimeout
	 * @param startIndex
	 * @return
	 */
	private static Integer getSlotOffset(AisExtractor extractor,
			int slotTimeout, int startIndex) {
		if (slotTimeout == 0)
			return extractor.getValue(startIndex + 5, startIndex + 19);
		else
			return null;
	}

	/**
	 * Returns sync state as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public int getSyncState() {
		return syncState;
	}

	/**
	 * Returns slot timeout as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public int getSlotTimeout() {
		return slotTimeout;
	}

	/**
	 * Returns received stations as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public Integer getReceivedStations() {
		return receivedStations;
	}

	/**
	 * Returns slot number as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public Integer getSlotNumber() {
		return slotNumber;
	}

	/**
	 * Returns hour UTC as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public Integer getHourUtc() {
		return hourUtc;
	}

	/**
	 * Returns minute UTC as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public Integer getMinuteUtc() {
		return minuteUtc;
	}

	/**
	 * Returns total minutes (60* hour + minute) in UTC.
	 * 
	 * @return
	 */
	public Integer getMinutesUtc() {
		return getMinutesUtc(slotTimeout, hourUtc, minuteUtc);
	}

	/**
	 * Returns minutes UTC (60* hour + minute) in UTC if slot timeout is 1, null
	 * otherwise.
	 * 
	 * @param slotTimeout
	 * @param hour
	 * @param minute
	 * @return
	 */
	private static Integer getMinutesUtc(int slotTimeout, Integer hour,
			Integer minute) {
		if (slotTimeout == 1) {
			return hour * 60 + minute;
		} else
			return null;
	}

	/**
	 * Returns slot offset as per 1371-4.pdf.
	 * 
	 * @return
	 */
	public Integer getSlotOffset() {
		return slotOffset;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Communications [startIndex=");
		builder.append(startIndex);
		builder.append(", syncState=");
		builder.append(syncState);
		builder.append(", slotTimeout=");
		builder.append(slotTimeout);
		builder.append(", receivedStations=");
		builder.append(receivedStations);
		builder.append(", slotNumber=");
		builder.append(slotNumber);
		builder.append(", hourUtc=");
		builder.append(hourUtc);
		builder.append(", minuteUtc=");
		builder.append(minuteUtc);
		builder.append(", slotOffset=");
		builder.append(slotOffset);
		builder.append("]");
		return builder.toString();
	}

}