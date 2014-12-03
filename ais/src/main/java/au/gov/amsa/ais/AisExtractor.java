package au.gov.amsa.ais;

/**
 * Utility class for extracting parts of an ais message as unsigned integers,
 * signed integers or strings.
 * 
 * @author dxm
 * 
 */
public class AisExtractor {

	private final String decodedMessage;

	/**
	 * Constructor. Does not do a minimum length check.
	 * 
	 * @param message
	 */
	public AisExtractor(String message) {
		this(message, null);
	}

	/**
	 * Constructor. If message once decoded is less than minLength then throws
	 * {@link AisParseException}.
	 * 
	 * @param message
	 * @param minLength
	 */
	public AisExtractor(String message, Integer minLength) {
		this.decodedMessage = Util.decodeMessage(message);
		if (minLength != null && decodedMessage.length() < minLength) {
			throw new AisParseException(
					AisParseException.NOT_CONSISTENT_DECODED_STRING
							+ ", length was " + decodedMessage.length()
							+ " and should be >=" + minLength);
		}
	}

	/**
	 * Returns the message id field (the first 6 characters of the decoded
	 * message).
	 * 
	 * @return
	 */
	public int getMessageId() {
		return getValue(0, 6, false);
	}

	/**
	 * Returns an unsigned integer value using the bits from character position
	 * start to position stop in the decoded message.
	 * 
	 * @param start
	 * @param stop
	 * @return
	 */
	public int getValue(int start, int stop) {
		return getValue(start, stop, false);
	}

	/**
	 * Returns the characters from position start to position stop in the
	 * decoded message.
	 * 
	 * @param start
	 * @param stop
	 * @return
	 */
	public String getString(int start, int stop) {
		try {
			return Util.getAsciiStringFrom6BitStr(decodedMessage.substring(
					start, stop));
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

	/**
	 * Returns a signed integer value using the bits from character position
	 * start to position stop in the decoded message.
	 * 
	 * @param start
	 * @param stop
	 * @return
	 */
	public int getSignedValue(int start, int stop) {
		return getValue(start, stop, true);
	}

	/**
	 * Returns an integer value using the bits from character position start to
	 * position stop in the decoded message. The returned value is a signed
	 * integer if the parameter <code>signed</code> is true otherwise the
	 * returned value is an unsigned integer.
	 * 
	 * @param start
	 * @param stop
	 * @param signed
	 * @return
	 */
	public int getValue(int start, int stop, boolean signed) {
		try {
			return Util.getValueByBinStr(decodedMessage.substring(start, stop),
					signed);
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

}
