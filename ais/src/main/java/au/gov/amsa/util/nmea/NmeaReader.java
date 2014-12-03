package au.gov.amsa.util.nmea;

/**
 * Provides an NMEA stream as an {@link Iterable} for convenience.
 * 
 * @author dxm
 * 
 */
public interface NmeaReader {

	/**
	 * Returns an {@link Iterable} of the NMEA messages.
	 * 
	 * @return
	 */
	Iterable<String> read();

}