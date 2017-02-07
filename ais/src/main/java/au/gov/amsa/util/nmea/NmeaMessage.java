package au.gov.amsa.util.nmea;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean to carry NMEA fields.
 * 
 * @author dxm
 * 
 */
public class NmeaMessage {

    private final LinkedHashMap<String, String> tags;
    private final List<String> items;
    private final Talker talker;
    private final SentenceInfo sentenceInfo;
    private final String checksum;

    /**
     * Constructor.
     * 
     * @param tags
     *            is a list of tags from the tag block section of an NMEA
     *            message
     * @param items
     *            is the list of columns from the NMEA message (not including
     *            the tag block) but including the checksum on the final column.
     */
    public NmeaMessage(LinkedHashMap<String, String> tags, List<String> items, String checksum) {
        this.tags = tags;
        this.items = items;
        if (!items.isEmpty() && items.get(0).length() >= 3)
            this.talker = NmeaUtil.getTalker((items.get(0).substring(1, 3)));
        else
            talker = Talker.UNKNOWN;
        this.sentenceInfo = getSentenceInfo(tags, items);
        this.checksum = checksum;
    }

    /**
     * Returns the 's:' value from the tag block.
     * 
     * @return
     */
    public String getSource() {
        return tags.get("s");
    }

    /**
     * Returns the 'c:' value from the tag block times 1000 to convert to
     * millis. Returns null if not present.
     * 
     * @return
     */
    public Long getUnixTimeMillis() {
        String time = tags.get("c");
        if (time == null)
            return null;
        else
            try {
                return Long.parseLong(time) * 1000;
            } catch (NumberFormatException e) {
                return null;
            }
    }

    /**
     * Returns the 'd:' value from the tag block.
     * 
     * @return
     */
    public String getDestination() {
        return tags.get("d");
    }

    /**
     * Returns the 'g:' value from the tag block.
     * 
     * @return
     */
    public String getSentenceGroupingFromTagBlock() {
        return tags.get("g");
    }

    /**
     * Returns the 'n:' value from the tag block.
     * 
     * @return
     */
    public Integer getLineCount() {
        String count = tags.get("n");
        if (count == null)
            return null;
        else
            return Integer.parseInt(count);
    }

    /**
     * Returns the 'r:' value from the tag block times 1000 to convert to
     * millis.
     * 
     * @return
     */
    public Long getRelativeTimeMillis() {
        String time = tags.get("r");
        if (time == null)
            return null;
        else
            return Long.parseLong(time) * 1000;
    }

    /**
     * Returns the 't:' value from the tag block.
     * 
     * @return
     */
    public String getText() {
        return tags.get("t");
    }

    /**
     * Returns a list of the NMEA items from the columns after the tag block.
     * 
     * @return
     */
    public List<String> getItems() {
        return items;
    }

    public LinkedHashMap<String, String> getTags() {
        return tags;
    }

    public Talker getTalker() {
        return talker;
    }

    public String toLine() {
        return NmeaUtil.createNmeaLine(tags, items);
    }

    public Integer getSentenceNumber() {
        if (sentenceInfo != null)
            return sentenceInfo.number;
        else
            return null;
    }

    public Integer getSentenceCount() {
        if (sentenceInfo != null)
            return sentenceInfo.count;
        else
            return null;
    }

    public String getSentenceGroupId() {
        if (sentenceInfo != null)
            return sentenceInfo.id;
        else
            return null;
    }

    /**
     * Returns a recalculated checksum.
     * 
     * @return calculated checksum
     */
    public String calculateChecksum() {
        return NmeaUtil.getChecksum(NmeaUtil.createNmeaLine(tags, items));
    }

    public String getChecksum() {
        return checksum;
    }

    private static final Pattern sentenceTagPattern = Pattern
            .compile("([1-9][0-9]*)G([1-9][0-9]*)");

    private static SentenceInfo getSentenceInfo(LinkedHashMap<String, String> tags,
            List<String> items) {
        try {
            String g = tags.get("g");
            if (g != null) {
                // old style NMEA 4.0.0 sentence grouping tag
                String[] parts = g.split("-");
                if (parts.length < 3)
                    throw new NmeaMessageParseException("not enough parts available in g tag");
                int number = Integer.parseInt(parts[0]);
                int count = Integer.parseInt(parts[1]);
                String id = parts[2];
                return new SentenceInfo(number, count, id);

            } else {
                // look for new style NMEA 4.1.0 sentence grouping tag (e.g.
                // 1G2)
                for (Entry<String, String> entry : tags.entrySet()) {
                    Matcher matcher = sentenceTagPattern.matcher(entry.getKey());
                    if (matcher.matches()) {
                        int number = Integer.parseInt(matcher.group(1));
                        int count = Integer.parseInt(matcher.group(2));
                        String id = entry.getValue();
                        // found it so return the sentence info
                        return new SentenceInfo(number, count, id);
                    }
                }
                // didn't find the grouping tag
                if (items.size() > 2 && isEncapsulationSentence(items)) {
                    int number = Integer.parseInt(items.get(2));
                    int count = Integer.parseInt(items.get(1));
                    String id = items.get(3);
                    return new SentenceInfo(number, count, id);
                } else
                    return null;
            }
        } catch (NumberFormatException e) {
            throw new NmeaMessageParseException(e.getMessage(), e);
        }
    }

    private static boolean isEncapsulationSentence(List<String> items) {
        return items.size() > 0 && items.get(0).startsWith("!");
    }

    private static class SentenceInfo {
        int number;
        int count;
        String id;

        public SentenceInfo(int number, int count, String id) {
            super();
            this.number = number;
            this.count = count;
            this.id = id;
        }
    }

    public boolean isSingleSentence() {
        return getSentenceCount() == null || getSentenceCount() == 1;
    }

    public Long getArrivalTimeMillis() {
        String time = tags.get("a");
        if (time == null)
            return null;
        else
            try {
                return Long.parseLong(time);
            } catch (NumberFormatException e) {
                return null;
            }
    }

}
