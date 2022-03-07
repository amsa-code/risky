package au.gov.amsa.ais;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaUtil;
import au.gov.amsa.util.nmea.Talker;

/**
 * Wraps an NMEA line containing an AIS message with accessor methods.
 * 
 * @author dxm
 * 
 */
public class AisNmeaMessage {

	private static final AisMessageParser aisParser = new AisMessageParser(
	        Util.getAisExtractorFactory());

	private final NmeaMessage nmea;

	private final String format;

	private final int fragmentCount;

	private final int fragmentNumber;

	private final String sequentialMessageId;

	private final String channel;

	private final String aisMessage;

	private final int padBits;

	/**
	 * Constructor.
	 * 
	 * @param line
	 *            is an NMEA line containing an AIS message.
	 */
	public AisNmeaMessage(String line) {
		this(NmeaUtil.parseNmea(validateLine(line)));
	}
	
	public static AisNmeaMessage from(String line) {
	    return new AisNmeaMessage(line);
	}

	private static String validateLine(String line) {
		try {
			if (!NmeaUtil.isValid(line))
				throw new AisParseException("invalid checksum: " + line + ", calculated checksum="
				        + NmeaUtil.getChecksum(line));
			return line;
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

	public AisNmeaMessage(NmeaMessage nmea) {
		try {
			this.nmea = nmea;
			if (nmea.getItems().size() < 7)
				throw new AisParseException("ais nmea line must have at least 7 columns:"
				        + nmea.getItems());
			format = getItem(0);
			fragmentCount = au.gov.amsa.util.Util.parseInt(getItem(1));
			fragmentNumber = au.gov.amsa.util.Util.parseInt(getItem(2));
			sequentialMessageId = getItem(3);
			channel = getItem(4);
			aisMessage = getItem(5);
			padBits = Integer.parseInt(nmea.getItems().get(6));
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

	/**
	 * Returns the tag block unix time value.
	 * 
	 * @return
	 */
	public Long getTime() {
		return nmea.getUnixTimeMillis();
	}

	/**
	 * Returns the {@link Talker} corresponding to the first two characters of
	 * the message format type (e.g. AIVDM -> AI). If not present or not recognized
	 * then returns {@code Talker.UNKNOWN}.
	 * 
	 * @return talker
	 */
	public Talker getTalker() {
		return nmea.getTalker();
	}

	/**
	 * Returns the string at column <code>index</code>, 0 based.
	 * 
	 * @param index
	 * @return
	 */
	private String getItem(int index) {
		return nmea.getItems().get(index);
	}

	/**
	 * Returns the NMEA format (from the first column after the tag block if it
	 * exists).
	 * 
	 * @return
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Returns the count of fragments.
	 * 
	 * @return
	 */
	public int getFragmentCount() {
		return fragmentCount;
	}

	/**
	 * Returns the current fragment number.
	 * 
	 * @return
	 */
	public int getFragmentNumber() {
		return fragmentNumber;
	}

	/**
	 * Returns the sequential message id.
	 * 
	 * @return
	 */
	public String getSequentialMessageId() {
		return sequentialMessageId;
	}

	/**
	 * Returns the channel.
	 * 
	 * @return
	 */
	public String getChannel() {
		return channel;
	}

	public int getPadBits() {
		return padBits;
	}

	/**
	 * Returns the parsed contents of column 5 of the AIS NMEA line.
	 * 
	 * @return
	 */
	public AisMessage getMessage() {
		AisMessage m = aisParser.parse(aisMessage, nmea.getSource(), padBits);
		return m;
	}

	public Timestamped<AisMessage> getTimestampedMessage(long defaultTime) {
		Long time = getTime();
		if (time == null)
			return Timestamped.create(getMessage(), defaultTime);
		else
			return Timestamped.create(getMessage(), getTime());
	}

    /**
     * Returns null if there is no timestamp otherwise returns a timestamped
     * message. Note that null is returned instead of using an Optional to reduce
     * allocation pressures.
     * 
     * @return timestamped message
     */
    public Timestamped<AisMessage> getTimestampedMessage() {
        Long time = getTime();
        if (time == null) {
            return null;
        } else {
            return Timestamped.create(getMessage(), getTime());
        }
    }

	/**
	 * Returns the checksum (last field in the NMEA line).
	 * 
	 * @return
	 */
	public String getChecksum() {
		return nmea.getChecksum();
	}

	public NmeaMessage getNmea() {
		return nmea;
	}
}
