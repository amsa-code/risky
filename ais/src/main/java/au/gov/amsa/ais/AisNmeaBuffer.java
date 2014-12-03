package au.gov.amsa.ais;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;

public class AisNmeaBuffer {

	private static final int AIS_MESSAGE_COL_NO = 5;
	private static final int MIN_NUM_COLS_FOR_LINE_TO_BE_AGGREGATED = 6;
	private final int columnToAggregate;
	private final int maxBufferSize;
	private final LinkedHashMultimap<String, NmeaMessage> buffer;

	public AisNmeaBuffer(int columnToAggregate, int maxBufferSize) {
		this.columnToAggregate = columnToAggregate;
		this.maxBufferSize = maxBufferSize;
		buffer = LinkedHashMultimap.create();
	}

	public AisNmeaBuffer(int maxBufferSize) {
		this(AIS_MESSAGE_COL_NO, maxBufferSize);
	}

	/**
	 * Returns the complete message only once the whole group of messages has
	 * arrived otherwise returns null.
	 * 
	 * @param nmea
	 * @return
	 */
	public synchronized NmeaMessage add(NmeaMessage nmea) {
		List<String> items = nmea.getItems();
		if (items.size() < MIN_NUM_COLS_FOR_LINE_TO_BE_AGGREGATED)
			return nmea;
		if (nmea.isSingleSentence()) {
			return nmea;
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
				NmeaMessage concatenatedMessage = concatenateMessages(list);
				buffer.removeAll(groupId);
				return concatenatedMessage;
			} else
				return null;
		}
	}

	/**
	 * Returns the aggregated message or if an {@link NmeaMessageParseException}
	 * occurs returns null.
	 * 
	 * @param list
	 * @return
	 */
	private NmeaMessage concatenateMessages(List<NmeaMessage> list) {
		NmeaMessage first = list.get(0);
		// concatenate column 5 and use row 1 tag block

		// copy cols so we can modify, don't want to affect the original nmea
		// message
		List<String> cols = Lists.newArrayList(first.getItems());

		// copy tags so we can modify
		LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>(
				first.getTags());

		StringBuilder s = new StringBuilder();
		for (NmeaMessage t : list) {
			s.append(t.getItems().get(columnToAggregate));
		}
		cols.set(columnToAggregate, s.toString());
		// set num sentences to be 1 and current sentence number to be 1
		cols.set(1, "1");
		cols.set(2, "1");

		tags.put("g", "1-1-" + first.getSentenceGroupId());
		try {
			NmeaMessage message = new NmeaMessage(tags, cols);
			return message;
		} catch (NmeaMessageParseException e) {
			return null;
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
