package au.gov.amsa.util.nmea;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.davidmoten.guavamini.Sets;

import au.gov.amsa.ais.AisParseException;

public final class NmeaUtil {

    private static final String[] EMPTY = new String[] {};

    private static final char BACKSLASH = '\\';

    private NmeaUtil() {
        // private constructor to prevent instantiation
    }

    static void forTestCoverageOnly() {
        new NmeaUtil();
    }

    private static final Set<Integer> invalidFieldCharacters = Sets.newHashSet(33, 36, 42, 44, 92,
            94, 126, 127);

    private static final Set<Character> validCharacterSymbols = createValidCharacterSymbols();

    private static Set<Character> createValidCharacterSymbols() {
        String s = "AaBCcDdEFfGgHhIJKkLlMmNnPQRrSsTtUuVWxyZ";
        Set<Character> set = Sets.newHashSet();
        for (char ch : s.toCharArray())
            set.add(ch);
        return set;
    }

    static boolean isValidFieldCharacter(char ch) {
        return ch <= 127 && ch >= 32 && !invalidFieldCharacters.contains((int) ch);
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
                    .equals(getChecksum(sentence));
        } catch (AisParseException e) {
            return false;
        }
    }

    public static String getChecksum(String sentence) {
        return getChecksum(sentence, true);
    }
    
    public static String getChecksumWhenHasNoTagBlock(String sentence) {
        return getChecksumWhenHasNoTagBlock(sentence, true, 0);
    }
    
    public static String getChecksum(String sentence, boolean ignoreLeadingDollarOrExclamation) {
        int startIndex;
        // Start after tag block
        if (sentence.startsWith("\\")) {
            startIndex = sentence.indexOf('\\', 1) + 1;
            if (startIndex == 0)
                throw new AisParseException("no closing \\ for tag block");
        } else
            startIndex = 0;
        return getChecksumWhenHasNoTagBlock(sentence, ignoreLeadingDollarOrExclamation, startIndex);
    }

    private static String getChecksumWhenHasNoTagBlock(String sentenceWithoutTagBlock, boolean ignoreLeadingDollarOrExclamation,
            int startIndex) {
        // Loop through all chars to get a checksum
        int checksum = 0;
        for (int i = startIndex; i < sentenceWithoutTagBlock.length(); i++) {
            char ch = sentenceWithoutTagBlock.charAt(i);
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
        return toUpperCaseHexString(checksum % 256);
    }
    
    private static final String[] hexes = IntStream //
            .range(0, 256) //
            .mapToObj(x -> {
                String s = Integer.toHexString(x).toUpperCase();
                if (s.length() == 1) {
                    s = "0" + s;
                }
                return s;
            }).collect(Collectors.toList()).toArray(EMPTY);
    
    private static String toUpperCaseHexString(int n) {
        return hexes[n];
    }

    private static NmeaMessageParser nmeaParser = new NmeaMessageParser();

    public static NmeaMessage parseNmea(String line) {
        return parseNmea(line, false);
    }
        
    public static NmeaMessage parseNmea(String line, boolean validateChecksum) {
        return nmeaParser.parse(line, validateChecksum);
    }
    
    public static String insertKeyValueInTagBlock(String line, String name, String value) {
        line = line.trim();
        if (line.startsWith("\\")) {
            // insert time into tag block, and adjust the
            // hash for the tag block
            int i = line.indexOf(BACKSLASH, 1);
            if (i == -1) {
                //return line unchanged
                return line;
            }
            if (i < 4) {
                // tag block not long enough to have a checksum");
                return line;
            }
            String content = line.substring(1, i - 3);
            StringBuilder s = new StringBuilder(content);
            s.append(",");
            s.append(name);
            s.append(":");
            s.append(value);
            String checksum = NmeaUtil.getChecksum(s.toString(), false);
            s.append('*');
            s.append(checksum);
            s.append(line.substring(i));
            s.insert(0, BACKSLASH);
            return s.toString();
        } else {
            return line;
        }
    }

    public static String supplementWithTime(String line, long arrivalTime) {
        line = line.trim();
        final String amendedLine;
        NmeaMessage m = parseNmea(line);
        Long t = m.getUnixTimeMillis();
        Long a = m.getArrivalTimeMillis();
        if (t == null) {
            // use arrival time if not present
            t = arrivalTime;

            // if has tag block
            if (line.startsWith("\\")) {
                // insert time into tag block, and adjust the
                // hash for the tag block
                int i = line.indexOf(BACKSLASH, 1);
                if (i == -1)
                    throw new RuntimeException(
                            "line starts with \\ but does not have closing tag block delimiter \\");
                if (i < 4)
                    throw new RuntimeException("tag block not long enough to have a checksum");
                String content = line.substring(1, i - 3);
                StringBuilder s = new StringBuilder(content);
                s.append(",");
                appendTimes(arrivalTime, t, s);
                String checksum = NmeaUtil.getChecksum(s.toString(), false);
                s.append('*');
                s.append(checksum);
                s.append(line.substring(i));
                s.insert(0, BACKSLASH);
                amendedLine = s.toString();
            } else {
                StringBuilder s = new StringBuilder();
                appendTimes(t, arrivalTime, s);
                String checksum = NmeaUtil.getChecksum(s.toString(), false);
                s.append("*");
                s.append(checksum);
                s.append(BACKSLASH);
                s.append(line);
                s.insert(0, BACKSLASH);
                amendedLine = s.toString();
            }
        } else if (a == null) {
            // must have a proper tag block because t != null

            // insert time into tag block, and adjust the
            // hash for the tag block
            int i = line.indexOf(BACKSLASH, 1);
            String content = line.substring(1, i - 3);
            StringBuilder s = new StringBuilder(content);
            s.append(",a:");
            s.append(arrivalTime);
            String checksum = NmeaUtil.getChecksum(s.toString(), false);
            s.append('*');
            s.append(checksum);
            s.append(line.substring(i));
            s.insert(0, BACKSLASH);
            amendedLine = s.toString();
        } else {
            amendedLine = line;
        }
        return amendedLine;

    }

    private static void appendTimes(long arrivalTime, Long t, StringBuilder s) {
        s.append("c:");
        s.append(t / 1000);
        s.append(",a:");
        s.append(arrivalTime);
    }
    
    private static final Map<String, Talker> talkers = Arrays.asList(Talker.values()).stream()
            .collect(Collectors.toMap(t -> t.name(), t -> t));
    
    public static Talker getTalker(String s) {
        if (s == null)
            return Talker.UNKNOWN;
        else {
            // don't use Talker.valueOf because it throws when s not valid Talker 
            // and thrown exceptions are bad for performance due allocations
            Talker t = talkers.get(s);
            if (t == null) {
                return Talker.UNKNOWN;
            } else {
                return t;
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

    public static String createNmeaLine(LinkedHashMap<String, String> tags, List<String> items) {
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

}
