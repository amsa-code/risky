package au.gov.amsa.ais;

import au.gov.amsa.util.SixBit;
import au.gov.amsa.util.SixBitException;

/**
 * Utility class for extracting parts of an ais message as unsigned integers,
 * signed integers or strings.
 * 
 * @author dxm
 * 
 */
public class AisExtractor {

	private final boolean[] bitSet;
	private final boolean[] calculated;
	private final int padBits;
	private final String message;

	/**
	 * Constructor. If message once decoded is less than minLength then throws
	 * {@link AisParseException}.
	 * 
	 * @param message
	 * @param minLength
	 */
	public AisExtractor(String message, Integer minLength, int padBits) {
		if (message.length() == 0)
			throw new AisParseException("message length cannot be 0");
		if (padBits > 6 || padBits < 0)
			throw new AisParseException("padBits must be between 0 and 6");
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
	public synchronized int getValue(int from, int to) {
		try {
			// is synchronized so that values of bitSet and calculated can be
			// lazily
			// calculated and safely published (thread safe).
			SixBit.convertSixBitToBits(message, padBits, bitSet, calculated, from, to);
			return (int) SixBit.getValue(from, to, bitSet);
		} catch (SixBitException e) {
			throw new AisParseException(e);
		}
	}

	/**
	 * Returns a signed integer value using the bits from character position
	 * start to position stop in the decoded message.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public synchronized int getSignedValue(int from, int to) {
		try {
			// is synchronized so that values of bitSet and calculated can be
			// lazily
			// calculated and safely published (thread safe).
			SixBit.convertSixBitToBits(message, padBits, bitSet, calculated, from, to);
			return (int) SixBit.getSignedValue(from, to, bitSet);
		} catch (SixBitException e) {
			throw new AisParseException(e);
		}
	}

	public synchronized String getString(int from, int to) {
		try {
			// is synchronized so that values of bitSet and calculated can be
			// lazily
			// calculated and safely published (thread safe).
			SixBit.convertSixBitToBits(message, padBits, bitSet, calculated, from, to);
			return SixBit.getString(from, to, bitSet);
		} catch (SixBitException e) {
			throw new AisParseException(e);
		}
	}

}
