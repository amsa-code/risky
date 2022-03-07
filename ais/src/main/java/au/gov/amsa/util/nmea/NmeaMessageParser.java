package au.gov.amsa.util.nmea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Parses NMEA messages.
 * 
 */
public class NmeaMessageParser {

    private static final String CHECKSUM_DELIMITER = "*";
    private static final char PARAMETER_DELIMITER = ',';
    private static final String CODE_DELIMITER = ":";

    public NmeaMessage parse(String line) {
        return parse(line, false);
    }
    
    /**
     * Return an {@link NmeaMessage} from the given NMEA line. If validate checksum
     * is true and the line fails the checksum check then an
     * {@link NmeaMessageParseException) is thrown.
     * 
     * @param line NMEA line (without EOL terminator)
     * @param validateChecksum if true then throws NmeaMessageParseException on invalid checksum
     * @return parsed message
     * @throws NmeaMessageParseException if tag block badly formed or no checksum
     *                                   found or if validateChecksum is true and
     *                                   line fails the checksum check
     */
    public NmeaMessage parse(String line, boolean validateChecksum) {
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();

        String remaining;
        if (line.startsWith("\\")) {
            int tagFinish = line.lastIndexOf('\\', line.length() - 1);
            if (tagFinish == -1)
                throw new NmeaMessageParseException(
                        "no matching \\ symbol to finish tag block: " + line);
            if (tagFinish == 0)
                throw new NmeaMessageParseException("tag block is empty or not terminated");
            // TODO if validateChecksum is true then validate the tag block checksum
            tags = extractTags(line.substring(1, tagFinish));
            remaining = line.substring(tagFinish + 1);
        } else
            remaining = line;

        List<String> items;
        String checksum;
        if (remaining.length() > 0) {
            if (!remaining.contains("*"))
                throw new NmeaMessageParseException("checksum delimiter * not found");
            items = getNmeaItems(remaining);
            checksum = remaining.substring(remaining.indexOf('*') + 1).trim();
            if (validateChecksum) {
                String calculatedChecksum = NmeaUtil.getChecksumWhenHasNoTagBlock(remaining);
                if (!checksum.equals(calculatedChecksum)) {
                    throw new NmeaMessageParseException("stated checksum does not match calculated");
                }
            }
        } else {
            items = Collections.emptyList();
            // TODO decide what value to put here
            checksum = "";
        }

        return new NmeaMessage(tags, items, checksum);
    }

    /**
     * Returns the items from the comma delimited NMEA line. The last item
     * always contains a * followed by a checksum. If * is not present
     * {@link IndexOutOfBoundsException} will be thrown.
     * 
     * @param line
     * @return
     */
    private static List<String> getNmeaItems(String line) {
        List<String> items = splitBy(line,
                PARAMETER_DELIMITER);
        // remove the checksum from the end
        String last = items.get(items.size()- 1);
        if (last.contains("*")) {
            items.set(items.size()- 1,  last.substring(0, last.lastIndexOf('*')));
        }
        return items;
    }

    private static List<String> splitBy(String s, char delimiter) {
        List<String> list = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == delimiter) {
                list.add(b.toString());
                b.setLength(0);
            } else {
                b.append(ch);
            }
        }
        list.add(b.toString());
        return list;
    }

    /**
     * Returns the tags from the tag block section of the message (NMEA v4.0).
     * If there is no tag block then returns an empty map.
     * 
     * @param s
     * @return
     */
    public static LinkedHashMap<String, String> extractTags(String s) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        int c = s.lastIndexOf(CHECKSUM_DELIMITER);
        if (c == -1) {
            return map;
        }
        s = s.substring(0, c);
        List<String> items = splitBy(s, PARAMETER_DELIMITER);
        for (String item : items) {
            int i = item.indexOf(CODE_DELIMITER);
            if (i == -1)
                throw new NmeaMessageParseException(
                        "TAG BLOCK parameter is not is format 'a:b' :" + s);
            map.put(item.substring(0, i), item.substring(i + 1));
        }
        return map;
    }
}
