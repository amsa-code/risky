package au.gov.amsa.ais;

import au.gov.amsa.util.SixBit;

/**
 * Utility class for extracting parts of an ais message as unsigned integers,
 * signed integers or strings.
 * 
 * @author dxm
 * 
 */
public class AisExtractor {

	// private final String decodedMessage;

	private final boolean[] bitSet;
	private final boolean[] calculated;
	private final int padBits;
	private final String message;

	/**
	 * Constructor. Does not do a minimum length check.
	 * 
	 * @param message
	 */
	public AisExtractor(String message, int padBits) {
		this(message, null, padBits);
	}

	/**
	 * Constructor. If message once decoded is less than minLength then throws
	 * {@link AisParseException}.
	 * 
	 * @param message
	 * @param minLength
	 */
	public AisExtractor(String message, Integer minLength, int padBits) {
		this.message = message;
		boolean[] bits = new boolean[message.length() * 6 - padBits];
		boolean[] calculated = new boolean[message.length()];
		this.bitSet = bits;
		this.calculated = calculated;
		this.padBits = padBits;
		if (minLength != null && bitSet.length < minLength) {
			throw new AisParseException(AisParseException.NOT_CONSISTENT_DECODED_STRING
			        + ", length was " + bitSet.length + " and should be >=" + minLength);
		}
	}

	/**
	 * Returns the message id field (the first 6 characters of the decoded
	 * message).
	 * 
	 * @return
	 */
	public int getMessageId() {
		return getValue(0, 6);
	}

	/**
	 * Returns an unsigned integer value using the bits from character position
	 * start to position stop in the decoded message.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public int getValue(int from, int to) {
		SixBit.sixBitToBits(message, padBits, bitSet, calculated, from, to);
		return (int) SixBit.getValue(from, to, bitSet);
	}

	/**
	 * Returns a signed integer value using the bits from character position
	 * start to position stop in the decoded message.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public int getSignedValue(int from, int to) {
		SixBit.sixBitToBits(message, padBits, bitSet, calculated, from, to);
		return (int) SixBit.getSignedValue(from, to, bitSet);
	}

	public String getString(int from, int to) {
		SixBit.sixBitToBits(message, padBits, bitSet, calculated, from, to);
		return SixBit.getString(from, to, bitSet);
	}

}
