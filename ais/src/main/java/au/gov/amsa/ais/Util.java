package au.gov.amsa.ais;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.TimeZone;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

/**
 * Ais utility methods.
 */
public final class Util {

	private Util() {
		// for test coverage
	}

	static void forTestCoverageOnly() {
		new Util();
	}

	/**
	 * Get a value for specified bits from the binary string.
	 * 
	 * @param fromBit
	 * @param toBit
	 * @return
	 * @throws AisParseException
	 */
	protected static int getValueByBinStr(String binaryString, boolean signed) {

		Integer value = Integer.parseInt(binaryString, 2);
		if (signed && binaryString.charAt(0) == '1') {
			char[] invert = new char[binaryString.length()];
			Arrays.fill(invert, '1');
			value ^= Integer.parseInt(new String(invert), 2);
			value += 1;
			value = -value;
		}

		return value;
	}

	static TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

	private static Charset ASCII_8_BIT_CHARSET = Charset.forName("ISO-8859-1");

	/**
	 * Returns decoded message from ascii 8 bit to 6 bit binary then to
	 * characters.
	 * 
	 * @param encodedMessage
	 * @return
	 */
	protected static String decodeMessage(String encodedMessage) {
		return getDecodedStr(ascii8To6bitBin(encodedMessage.getBytes(ASCII_8_BIT_CHARSET)));
	}

	/**
	 * Returns conversion of ASCII-coded character to 6-bit binary byte array.
	 * 
	 * @param toDecBytes
	 * @return decodedBytes
	 */
	@VisibleForTesting
	static byte[] ascii8To6bitBin(byte[] toDecBytes) {

		byte[] convertedBytes = new byte[toDecBytes.length];
		int sum = 0;
		int _6bitBin = 0;

		for (int i = 0; i < toDecBytes.length; i++) {
			sum = 0;
			_6bitBin = 0;

			if (toDecBytes[i] < 48) {
				throw new AisParseException(AisParseException.INVALID_CHARACTER + " "
				        + (char) toDecBytes[i]);
			} else {
				if (toDecBytes[i] > 119) {
					throw new AisParseException(AisParseException.INVALID_CHARACTER + " "
					        + (char) toDecBytes[i]);
				} else {
					if (toDecBytes[i] > 87) {
						if (toDecBytes[i] < 96) {
							throw new AisParseException(AisParseException.INVALID_CHARACTER + " "
							        + (char) toDecBytes[i]);
						} else {
							sum = toDecBytes[i] + 40;
						}
					} else {
						sum = toDecBytes[i] + 40;
					}
					if (sum != 0) {
						if (sum > 128) {
							sum += 32;
						} else {
							sum += 40;
						}
						_6bitBin = sum & 0x3F;
						convertedBytes[i] = (byte) _6bitBin;
					}
				}
			}
		}

		return convertedBytes;
	}

	/**
	 * Get decoded string from bytes.
	 * 
	 * @param decBytes
	 */
	private static String getDecodedStr(byte[] decBytes) {

		// prepare StringBuilder with capacity being the smallest power of 2
		// greater than decBytes.length*6
		int n = decBytes.length * 6;
		int capacity = leastPowerOf2GreaterThanOrEqualTo(n);
		StringBuilder decStr = new StringBuilder(capacity);

		for (int i = 0; i < decBytes.length; i++) {

			int decByte = decBytes[i];
			String bitStr = Integer.toBinaryString(decByte);

			int padding = Math.max(0, 6 - bitStr.length());

			for (int j = 0; j < padding; j++) {
				decStr.append('0');
			}

			for (int j = 0; j < 6 - padding; j++) {
				decStr.append(bitStr.charAt(j));
			}
		}
		return decStr.toString();
	}

	static int leastPowerOf2GreaterThanOrEqualTo(int n) {
		return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
	}

	/**
	 * Decode 6 bit String to standard ASCII String
	 * 
	 * Input is a binary string of 0 and 1 each 6 bit is a character that will
	 * be converted to the standard ASCII character
	 * 
	 * @param str
	 * @return
	 */
	protected static String getAsciiStringFrom6BitStr(String str) {

		StringBuilder txt = new StringBuilder();
		for (int i = 0; i < str.length(); i = i + 6) {
			byte _byte = (byte) Integer.parseInt(str.substring(i, i + 6), 2);
			_byte = convert6BitCharToStandardAscii(_byte);
			char convChar = (char) _byte;

			if (convChar == '@') {
				break;
			}
			txt.append((char) _byte);
		}

		return txt.toString().trim();
	}

	/**
	 * Convert one 6 bit ASCII character to 8 bit ASCII character
	 * 
	 * @param byteToConvert
	 * @return
	 */
	@VisibleForTesting
	static byte convert6BitCharToStandardAscii(byte byteToConvert) {

		byte b = 0;
		if (byteToConvert < 32) {
			b = (byte) (byteToConvert + 64);
		} else if (byteToConvert < 63) {
			b = byteToConvert;
		}

		return b;
	}

	/**
	 * Check lat lon are withing allowable range as per 1371-4.pdf. Note that
	 * values of long=181, lat=91 have special meaning.
	 * 
	 * @param lat
	 * @param lon
	 */
	public static void checkLatLong(double lat, double lon) {
		checkArgument(lon <= 181.0, "longitude out of range " + lon);
		checkArgument(lon > -180.0, "longitude out of range " + lon);
		checkArgument(lat <= 91.0, "latitude out of range " + lat);
		checkArgument(lat > -90.0, "latitude out of range " + lat);
	}

	/**
	 * Check lat is within allowable range as per 1371-4.pdf. Note that value of
	 * lat=91 has special meaning.
	 * 
	 * @param lat
	 */
	public static void checkLat(double lat) {
		checkArgument(lat <= 91.0, "latitude out of range ");
		checkArgument(lat > -90.0, "latitude out of range ");
	}

	/**
	 * Check lon is within allowable range as per 1371-4.pdf. Note that value of
	 * long=181, has special meaning.
	 * 
	 * @param lon
	 */
	public static void checkLong(double lon) {
		checkArgument(lon <= 181.0, "longitude out of range");
		checkArgument(lon > -180.0, "longitude out of range");
	}

	/**
	 * Throws an AisParseException with given message if b is false.
	 * 
	 * @param b
	 * @param message
	 */
	public static void checkArgument(boolean b, String message) {
		if (!b)
			throw new AisParseException(message);
	}

	private static AisExtractorFactory extractorFactory = new AisExtractorFactory() {

		@Override
		public AisExtractor create(String message, int minLength, int padBits) {
			return new AisExtractor(message, minLength, padBits);
		}

	};

	/**
	 * Returns singleton {@link AisExtractorFactory}.
	 * 
	 * @return
	 */
	public static AisExtractorFactory getAisExtractorFactory() {
		return extractorFactory;
	}

	/**
	 * Check message id corresponds to one of the given list of message types.
	 * 
	 * @param messageId
	 * @param messageTypes
	 */
	public static void checkMessageId(int messageId, AisMessageType... messageTypes) {
		boolean found = false;
		for (AisMessageType messageType : messageTypes) {
			if (messageType.getId() == messageId)
				found = true;
		}
		if (!found) {
			StringBuffer s = new StringBuffer();
			for (AisMessageType messageType : messageTypes) {
				if (s.length() > 0)
					s.append(",");
				s.append(messageType.getId() + "");
			}
			checkArgument(found, "messageId must be in [" + s + "]  but was " + messageId);
		}
	}

	/**
	 * Returns true if and only if given integers are equal. This was extracted
	 * to assist in source code branch coverage.
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	public static boolean areEqual(int i, int j) {
		return i == j;
	}

	/**
	 * Returns true if and only if given messageId corresponds to a class A
	 * position report (message ids 1,2,3).
	 * 
	 * @param messageId
	 * @return
	 */
	public static boolean isClassAPositionReport(int messageId) {
		return messageId == 1 || messageId == 2 || messageId == 3;
	}

}
