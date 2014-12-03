package au.gov.amsa.ais;

/**
 * Receives the results of Nmea stream processing.
 * 
 * @author dxm
 * 
 */
public interface NmeaStreamProcessorListener {

	/**
	 * Message has arrived with given timestamp.
	 * 
	 * @param line
	 *            nmea line
	 * @param time
	 *            in epoch ms
	 */
	void message(String line, long time);

	/**
	 * Message has arrived and could not be associated with a timestamp other
	 * than the arrival time.
	 * 
	 * @param line
	 *            nmea line
	 * @param arrivalTime
	 *            in epoch ms
	 */
	void timestampNotFound(String line, Long arrivalTime);

	/**
	 * Message has arrived that could not be parsed.
	 * 
	 * @param line
	 *            nmea line
	 * @param arrivalTime
	 *            in epoch ms
	 * @param message
	 *            the parse error message
	 */
	void invalidNmea(String line, long arrivalTime, String message);
}
