package au.gov.amsa.ais;

/**
 * The AIS message types as per 1371-4.pdf. Not all are listed here.
 * 
 * @author dxm
 * 
 */
public enum AisMessageType {

	POSITION_REPORT_SCHEDULED(1), POSITION_REPORT_ASSIGNED(2), POSITION_REPORT_SPECIAL(
			3), BASE_STATION_REPORT(4), STATIC_AND_VOYAGE_RELATED_DATA(5), ADDRESSED_BINARY_MESSAGE(
			6), BINARY_ACKNOWLEDGE(7), BINARY_BROADCAST_MESSAGE(8), STANDARD_SAR_AIRCRAFT_POSITION_REPORT(
			9), UTC_AND_DATE_INQUIRY(10), UTC_AND_DATE_RESPONSE(11), ADDRESSED_SAFETY_RELATED_MESSAGE(
			12), POSITION_REPORT_CLASS_B(18), POSITION_REPORT_CLASS_B_EXTENDED(
			19), ATON_REPORT(21), STATIC_DATA_REPORT(24);

	private final int id;

	/**
	 * Private constructor.
	 * 
	 * @param id
	 */
	private AisMessageType(int id) {
		this.id = id;
	}

	/**
	 * Returns the message id as per 1371-3.pdf.
	 * 
	 * @return
	 */
	public int getId() {
		return id;
	}
	
}
