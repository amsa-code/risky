package au.gov.amsa.ais;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;

public class AisNmeaBuffer {

    private static final int AIS_MESSAGE_COL_NO = 5;
    private static final int MIN_NUM_COLS_FOR_LINE_TO_BE_AGGREGATED = 6;
    private static final int COLUMN_TO_AGGREGATE = AIS_MESSAGE_COL_NO;
    private final int maxBufferSize;
    private final LinkedHashMultimap<String, NmeaMessage> buffer;
    private final AtomicBoolean adding = new AtomicBoolean();

    public AisNmeaBuffer(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        this.buffer = LinkedHashMultimap.create();
    }

    /**
     * Returns the complete message only once the whole group of messages has
     * arrived otherwise returns null.
     * 
     * @param nmea
     * @return
     */
    public Optional<List<NmeaMessage>> add(NmeaMessage nmea) {
        // use compare-and-swap semantics instead of synchronizing to squeak a
        // bit more performance out of this. Contention is expected to be low so
        // this should help.
        while (true) {
            if (adding.compareAndSet(false, true)) {
                Optional<List<NmeaMessage>> result = doAdd(nmea);
                adding.set(false);
                return result;
            }
        }
    }

    private Optional<List<NmeaMessage>> doAdd(NmeaMessage nmea) {
        List<String> items = nmea.getItems();
        if (items.size() > 0 && items.size() < MIN_NUM_COLS_FOR_LINE_TO_BE_AGGREGATED)
            return Optional.of(Collections.singletonList(nmea));
        if (nmea.isSingleSentence()) {
            return Optional.of(Collections.singletonList(nmea));
        } else {
            String groupId = nmea.getSentenceGroupId();
            buffer.put(groupId, nmea);
            // trim the oldest if we have reached max buffer size
            if (buffer.size() > maxBufferSize)
                buffer.removeAll(buffer.keySet().iterator().next());
            int numGroupMessages = nmea.getSentenceCount();
            int numGroupMessagesSoFar = buffer.get(groupId).size();
            if (numGroupMessagesSoFar == numGroupMessages) {
                // we have all messages in that group now so concatenate
                List<NmeaMessage> list = orderMessages(buffer.get(groupId));
                // NmeaMessage concatenatedMessage = concatenateMessages(list);
                buffer.removeAll(groupId);
                return Optional.of(list);
            } else
                return Optional.absent();
        }
    }

    /**
     * Returns the aggregated message or if an {@link NmeaMessageParseException}
     * occurs returns null.
     * 
     * @param list
     * @return
     */
    public static Optional<NmeaMessage> concatenateMessages(List<NmeaMessage> list) {
        if (list.size() == 1)
            return Optional.of(list.get(0));

        NmeaMessage first = list.get(0);
        // concatenate column 5 and use row 1 tag block

        // copy tags so we can modify
        LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>(first.getTags());

        StringBuilder s = new StringBuilder();
        List<String> foundItems = null;
        for (NmeaMessage t : list) {
            if (!t.getItems().isEmpty()) {
                // parse the num and count values from the NMEA colums
                // an VSI message will fail this check
                try {
                    Integer.parseInt(t.getItems().get(1));
                    Integer.parseInt(t.getItems().get(2));
                    s.append(t.getItems().get(COLUMN_TO_AGGREGATE));
                    foundItems = t.getItems();
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        if (foundItems == null) {
            return Optional.absent();
        }
        // copy cols so we can modify, don't want to affect the original nmea
        // message
        List<String> cols = Lists.newArrayList(foundItems);
        cols.set(COLUMN_TO_AGGREGATE, s.toString());
        // set num sentences to be 1 and current sentence number to be 1
        cols.set(1, "1");
        cols.set(2, "1");

        tags.put("g", "1-1-" + first.getSentenceGroupId());
        try {
            String checksum = NmeaUtil.getChecksum(NmeaUtil.createNmeaLine(tags, cols));
            NmeaMessage message = new NmeaMessage(tags, cols, checksum);
            return Optional.of(message);
        } catch (NmeaMessageParseException e) {
            return Optional.absent();
        }
    }

    private static List<NmeaMessage> orderMessages(Collection<NmeaMessage> c) {
        List<NmeaMessage> list = Lists.newArrayList(c);
        Collections.sort(list, new Comparator<NmeaMessage>() {
            @Override
            public int compare(NmeaMessage a, NmeaMessage b) {
                return a.getSentenceNumber().compareTo(b.getSentenceNumber());
            }
        });
        return list;
    }

    public int size() {
        return buffer.size();
    }
}
