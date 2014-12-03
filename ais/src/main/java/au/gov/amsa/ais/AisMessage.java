package au.gov.amsa.ais;

/**
 * An AIS Message according to ITU R M 1371-4 ( a copy is in
 * cts-adapter-ais/docs).
 * 
 * @author dxm
 * 
 */
public interface AisMessage {
	/**
	 * Returns the ais message id. For example Class A Position reports are
	 * either 1, 2, or 3.
	 * 
	 * @return
	 */
	int getMessageId();

	/**
	 * Returns the source of the ais message. The source is not available in the
	 * ais message itself but may be provided in the tag block of the NMEA
	 * message that contains the ais message.
	 * 
	 * @return
	 */
	String getSource();
}
