package au.gov.amsa.ais;

import dk.dma.ais.binary.SixbitException;

/**
 * Utility class for extracting parts of an ais message as unsigned integers,
 * signed integers or strings.
 * 
 * @author dxm
 * 
 */
public class AisExtractor {

	private final String decodedMessage;

	private boolean[] bitSet;

	/** Precompiled list of int to six bit mappings. */
	private static final int[] INT_TO_SIX_BIT = createIntToSixBit();

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
		this.bitSet = sixBitToBits(message, padBits);
		this.decodedMessage = Util.decodeMessage(message);
		if (minLength != null && decodedMessage.length() < minLength) {
			throw new AisParseException(AisParseException.NOT_CONSISTENT_DECODED_STRING
			        + ", length was " + decodedMessage.length() + " and should be >=" + minLength);
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
			return Util.getAsciiStringFrom6BitStr(decodedMessage.substring(start, stop));
		} catch (RuntimeException e) {
			throw new AisParseException(e);
		}
	}

	public String getString2(int from, int to) throws SixbitException {
		int len = to - from;
		char[] resStr = new char[len];
		for (int i = 0; i < len; i++) {
			resStr[i] = (char) intToascii((char) getValue(i, i + 6));
		}
		return new String(resStr);
	}

	/**
	 * Convert six bit int value to character
	 * 
	 * @param val
	 * @return
	 * @throws SixbitException
	 */
	public static int intToascii(int val) throws SixbitException {
		if (val > 63) {
			throw new SixbitException("Char value " + val + " not allowed");
		}
		if (val < 32) {
			return val + 64;
		} else {
			return val;
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
		if (true)
			return (int) getVal(start, stop - 1, bitSet);
		else
			try {
				return Util.getValueByBinStr(decodedMessage.substring(start, stop), signed);
			} catch (RuntimeException e) {
				throw new AisParseException(e);
			}
	}

	private static int[] createIntToSixBit() {
		int[] toSixbit = new int[256 * 256]; // we actually only use 256, but we
		                                     // parse chars around instead of
		                                     // bytes
		for (int chr = 0; chr < toSixbit.length; chr++) {
			if (chr < 48 || chr > 119 || chr > 87 && chr < 96) {
				toSixbit[chr] = -1;
			} else if (chr < 0x60) {
				toSixbit[chr] = chr - 48 & 0x3F;
			} else {
				toSixbit[chr] = chr - 56 & 0x3F;
			}
		}
		return toSixbit;
	}

	/**
	 * Append bits from a sixbit encoded string
	 * 
	 * @param str
	 * @param padBits
	 * @throws SixbitException
	 */
	private static boolean[] sixBitToBits(String str, int padBits) {
		if (str.length() == 0) {
			return new boolean[1024];
		}
		int len = str.length() * 6 - padBits;
		boolean[] bitSet = new boolean[len];

		int slen = str.length() - 1;
		int index = 0;
		for (int i = 0; i < slen; i++) {
			char chr = str.charAt(i);
			int binVal = INT_TO_SIX_BIT[chr];
			if (binVal == -1) {
				throw new RuntimeException("Illegal sixbit ascii char: " + chr);
			}
			bitSet[index] = (binVal & 32) > 0;
			bitSet[index + 1] = (binVal & 16) > 0;
			bitSet[index + 2] = (binVal & 8) > 0;
			bitSet[index + 3] = (binVal & 4) > 0;
			bitSet[index + 4] = (binVal & 2) > 0;
			bitSet[index + 5] = (binVal & 1) > 0;
			index += 6;
		}

		// Process the last char which might be padded
		char chr = str.charAt(slen);
		int binVal = INT_TO_SIX_BIT[chr];
		if (binVal == -1) {
			throw new RuntimeException("Illegal sixbit ascii char: " + chr);
		}
		int bits = 6 - padBits;
		switch (bits) {
		case 6:
			bitSet[index + 5] = (binVal & 1) > 0;
		case 5:
			bitSet[index + 4] = (binVal & 2) > 0;
		case 4:
			bitSet[index + 3] = (binVal & 4) > 0;
		case 3:
			bitSet[index + 2] = (binVal & 8) > 0;
		case 2:
			bitSet[index + 1] = (binVal & 16) > 0;
		case 1:
			bitSet[index] = (binVal & 32) > 0;
		}
		return bitSet;
	}

	private static long getVal(int from, int to, boolean[] bitSet) {
		if (to >= bitSet.length) {
			throw new RuntimeException(bitSet.length + " is not enough bits. At least " + to
			        + " expected.");
		}
		long val = 0;
		long powMask = 1;
		for (int i = to; i >= from; i--) {
			if (bitSet[i]) {
				val += powMask;
			}
			powMask <<= 1;
		}
		return val;
	}

}
