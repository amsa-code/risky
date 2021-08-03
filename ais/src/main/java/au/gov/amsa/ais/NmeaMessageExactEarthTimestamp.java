package au.gov.amsa.ais;

import static java.lang.Integer.parseInt;

import java.util.Calendar;
import java.util.regex.Pattern;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;

/**
 * Parses a custom ExactEarth NMEA line for AMSA so that we can obtain the
 * actual timestamp of an AIS Position Report (types 1,2,3).
 * 
 * @author dxm
 * 
 */
public class NmeaMessageExactEarthTimestamp {

	private final NmeaMessage nmea;
	private final long time;
	private final String followingSequenceChecksum;

	/**
	 * Constructor.
	 * 
	 * @param line
	 */
	public NmeaMessageExactEarthTimestamp(String line) {
		nmea = NmeaUtil.parseNmea(line);
		try {
			Util.checkArgument(isPghp(line), "not an ExactEarth timestamp: "
					+ line);
			getFollowingSequenceChecksum();
			int year = parseInt(getItem(2));
			int month = parseInt(getItem(3));
			int day = parseInt(getItem(4));
			int hour = parseInt(getItem(5));
			int minute = parseInt(getItem(6));
			int second = parseInt(getItem(7));
			int millisecond = parseInt(getItem(8));
			time = getTime(year, month, day, hour, minute, second, millisecond);
			followingSequenceChecksum = extractFollowingSequenceChecksum();
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

	private static final Pattern pghpPattern = Pattern
			.compile("^(\\\\.*\\\\)?\\$PGHP.*$");

	/**
	 * Returns true if and only if the given NMEA line is a PGHP line. Remember
	 * that the line can start with a tag block.
	 * 
	 * @param line
	 * @return
	 */
	@VisibleForTesting
	static boolean isPghp(String line) {
		if (line == null)
			return false;
		else
			// return Pattern.matches("^(\\\\.*\\\\)?\\$PGHP.*$", line);
			return pghpPattern.matcher(line).matches();
	}

	/**
	 * Returns true if the given line is a custom ExactEarth timestamp.
	 * 
	 * @param line
	 * @return
	 */
	public static boolean isExactEarthTimestamp(String line) {
		try {
			new NmeaMessageExactEarthTimestamp(line);
			return true;
		} catch (AisParseException e) {
			return false;
		} catch (NmeaMessageParseException e) {
			return false;
		}
	}

	/**
	 * Returns the item at index, zero based.
	 * 
	 * @param index
	 * @return
	 */
	private String getItem(int index) {
		return nmea.getItems().get(index);
	}

	/**
	 * Returns the epoch time in ms.
	 * 
	 * @return
	 */
	private static long getTime(int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		Calendar cal = Calendar.getInstance(Util.TIME_ZONE_UTC);
		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, millisecond);
		return cal.getTimeInMillis();
	}

	/**
	 * Returns the time in epoch ms.
	 * 
	 * @return
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Returns the checksum of the message that this timestamp line refers to.
	 * 
	 * @return
	 */
	public String getFollowingSequenceChecksum() {
		return followingSequenceChecksum;
	}

	/**
	 * Returns the checksum of the message that this timestamp refers to.
	 * 
	 * @return
	 */
	private String extractFollowingSequenceChecksum() {
		return getItem(13);
	}
}
