package au.gov.amsa.util.nmea;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import rx.Observable;
import rx.observables.StringObservable;
import au.gov.amsa.ais.AisParseException;

import com.google.common.collect.Sets;

public final class NmeaUtil {

	private NmeaUtil() {
		// private constructor to prevent instantiation
	}

	static void forTestCoverageOnly() {
		new NmeaUtil();
	}

	private static final Set<Integer> invalidFieldCharacters = Sets.newHashSet(
			33, 36, 42, 44, 92, 94, 126, 127);

	private static final Set<Character> validCharacterSymbols = createValidCharacterSymbols();

	private static Set<Character> createValidCharacterSymbols() {
		String s = "AaBCcDdEFfGgHhIJKkLlMmNnPQRrSsTtUuVWxyZ";
		Set<Character> set = Sets.newHashSet();
		for (char ch : s.toCharArray())
			set.add(ch);
		return set;
	}

	static boolean isValidFieldCharacter(char ch) {
		return ch <= 127 && ch >= 32
				&& !invalidFieldCharacters.contains((int) ch);
	}

	static boolean isValidCharacterSymbol(char ch) {
		return validCharacterSymbols.contains(ch);
	}

	/**
	 * Returns true if and only if the sentence's checksum matches the
	 * calculated checksum.
	 * 
	 * @param sentence
	 * @return
	 */
	public static boolean isValid(String sentence) {
		// Compare the characters after the asterisk to the calculation

		try {
			return sentence.substring(sentence.lastIndexOf("*") + 1)
					.equalsIgnoreCase(getChecksum(sentence));
		} catch (AisParseException e) {
			return false;
		}
	}

	public static String getChecksum(String sentence) {
		return getChecksum(sentence, true);
	}

	public static String getChecksum(String sentence,
			boolean ignoreLeadingDollarOrExclamation) {

		int startIndex;
		// Start after tag block
		if (sentence.startsWith("\\")) {
			startIndex = sentence.indexOf('\\', 1) + 1;
			if (startIndex == 0)
				throw new AisParseException("no closing \\ for tag block");
		} else
			startIndex = 0;
		// Loop through all chars to get a checksum
		int checksum = 0;
		for (int i = startIndex; i < sentence.length(); i++) {
			char ch = sentence.charAt(i);
			if (ignoreLeadingDollarOrExclamation && (ch == '$' || ch == '!')) {
				// Ignore the dollar sign
			} else if (ch == '*') {
				// Stop processing before the asterisk
				break;
			} else {
				// Is this the first value for the checksum?
				if (checksum == 0) {
					// Yes. Set the checksum to the value
					checksum = ch;
				} else {
					// No. XOR the checksum with this character's value
					checksum = checksum ^ ch;
				}
			}
		}
		// Return the checksum formatted as a two-character hexadecimal
		String s = Integer.toHexString(checksum % 256);
		if (s.length() == 1)
			s = "0" + s;
		return s.toUpperCase();
	}

	private static NmeaMessageParser nmeaParser = new NmeaMessageParser();

	public static NmeaMessage parseNmea(String line) {
		return nmeaParser.parse(line);
	}

	public static Talker getTalker(String s) {
		if (s == null)
			return null;
		else {
			try {
				return Talker.valueOf(s);
			} catch (RuntimeException e) {
				return Talker.UNKNOWN;
			}
		}
	}

	public static String createTagBlock(LinkedHashMap<String, String> tags) {
		if (tags == null || tags.size() == 0)
			return "";
		StringBuilder s = new StringBuilder(128);
		s.append("\\");
		int startChecksum = s.length();
		boolean first = true;
		for (Entry<String, String> entry : tags.entrySet()) {
			if (!first)
				s.append(",");
			s.append(entry.getKey());
			s.append(":");
			s.append(entry.getValue());
			first = false;
		}
		String checksum = NmeaUtil.getChecksum(s.substring(startChecksum));
		s.append("*");
		s.append(checksum);
		s.append("\\");
		return s.toString();
	}

	public static String createNmeaLine(LinkedHashMap<String, String> tags,
			List<String> items) {
		StringBuilder s = new StringBuilder(40);
		s.append(createTagBlock(tags));
		int startForChecksum = s.length();
		boolean first = true;
		for (String item : items) {
			if (!first)
				s.append(",");
			s.append(item);
			first = false;
		}
		String checksum = NmeaUtil.getChecksum(s.substring(startForChecksum));
		s.append("*");
		s.append(checksum);

		return s.toString();
	}

	public static Observable<String> nmeaLines(InputStream is) {
		return StringObservable.split(StringObservable
				.from(new InputStreamReader(is, Charset.forName("UTF-8"))),
				"\n");
	}

}
