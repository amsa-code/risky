package au.gov.amsa.ais;

import static au.gov.amsa.ais.NmeaMessageExactEarthTimestamp.isExactEarthTimestamp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.guavamini.Lists;
import com.github.davidmoten.guavamini.Sets;
import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;

/**
 * Extracts time from a message if possible and reports results to listeners.
 * 
 * @author dxm
 * 
 */
public class NmeaStreamProcessor {

	private static final int DEFAULT_NMEA_BUFFER_SIZE = 100;
	private static final int DEFAULT_LOG_COUNT_FREQUENCY = 100000;
	private static Logger log = LoggerFactory
			.getLogger(NmeaStreamProcessor.class);
	private static final long MAXIMUM_ARRIVAL_TIME_DIFFERENCE_MS = 1000;
	private final NmeaStreamProcessorListener listener;
	private final List<LineAndTime> lines = Lists.newArrayList();
	private final boolean matchWithTimestampLine;
	private final AtomicLong count = new AtomicLong();
	private final long logCountFrequency;
	private final AisNmeaBuffer nmeaBuffer;

	/**
	 * Constructor.
	 * 
	 * @param listener
	 * @param matchWithTimestampLine
	 */
	public NmeaStreamProcessor(NmeaStreamProcessorListener listener,
			boolean matchWithTimestampLine, long logCountFrequency,
			int nmeaBufferSize) {
		this.listener = listener;
		this.matchWithTimestampLine = matchWithTimestampLine;
		this.logCountFrequency = logCountFrequency;
		this.nmeaBuffer = new AisNmeaBuffer(nmeaBufferSize);
	}

	/**
	 * Constructor.
	 * 
	 * @param listener
	 * @param matchWithTimestampLine
	 */
	public NmeaStreamProcessor(NmeaStreamProcessorListener listener,
			boolean matchWithTimestampLine, long logCountFrequency) {
		this(listener, matchWithTimestampLine, logCountFrequency,
				DEFAULT_NMEA_BUFFER_SIZE);
	}

	public NmeaStreamProcessor(NmeaStreamProcessorListener listener,
			boolean matchWithTimestampLine) {
		this(listener, matchWithTimestampLine, DEFAULT_LOG_COUNT_FREQUENCY,
				DEFAULT_NMEA_BUFFER_SIZE);
	}

	/**
	 * Handles the arrival of a new NMEA line and assumes its arrival time is
	 * now.
	 * 
	 * @param line
	 */
	public void line(String line) {
		line(line, System.currentTimeMillis());
	}

	/**
	 * Handles the arrival of a new NMEA line at the given arrival time.
	 * 
	 * @param line
	 * @param arrivalTime
	 */
	void line(String line, long arrivalTime) {

		if (count.incrementAndGet() % logCountFrequency == 0)
			log.info("count=" + count.get() + ",buffer size=" + lines.size());

		NmeaMessage nmea;
		try {
			nmea = NmeaUtil.parseNmea(line);
		} catch (NmeaMessageParseException e) {
			listener.invalidNmea(line, arrivalTime, e.getMessage());
			return;
		}

		// if is multi line message then don't report to listener till last
		// message in sequence has been received.
		if (!nmea.isSingleSentence()) {
			Optional<List<NmeaMessage>> messages = nmeaBuffer.add(nmea);
			if (messages.isPresent()) {
				Optional<NmeaMessage> joined = AisNmeaBuffer
						.concatenateMessages(messages.get());
				if (joined.isPresent()) {
					if (joined.get().getUnixTimeMillis() != null)
						listener.message(joined.get().toLine(), joined.get()
								.getUnixTimeMillis());
					else
						listener.message(joined.get().toLine(), arrivalTime);
				}
				// TODO else report error, might need to change signature of
				// listener to handle problem with multi-line message
			}
			return;
		}

		if (nmea.getUnixTimeMillis() != null) {
			listener.message(line, nmea.getUnixTimeMillis());
			return;
		}

		if (!matchWithTimestampLine) {
			listener.message(line, arrivalTime);
			return;
		}

		if (!NmeaUtil.isValid(line))
			return;

		addLine(line, arrivalTime);
		log.debug("buffer lines=" + lines.size());
		Integer earliestTimestampLineIndex = getEarliestTimestampLineIndex(lines);
		Set<Integer> removeThese;
		if (earliestTimestampLineIndex != null) {
			removeThese = matchWithClosestAisMessageIfBufferLargeEnough(
					arrivalTime, earliestTimestampLineIndex);
		} else
			removeThese = findExpiredIndexesBeforeIndex(lastIndex());
		TreeSet<Integer> orderedIndexes = new TreeSet<>(removeThese);
		for (int index : orderedIndexes.descendingSet()) {
			removeLineWithIndex(index);
		}
	}

	/**
	 * Returns the last index of the buffered lines.
	 * 
	 * @return
	 */
	private int lastIndex() {
		return lines.size() - 1;
	}

	/**
	 * Returns the list of those indexes that can be removed from the buffer
	 * because they have a timestamp more than
	 * MAXIMUM_ARRIVAL_TIME_DIFFERENCE_MS from the arrival time of the given
	 * index.
	 * 
	 * @param index
	 * @return
	 */
	private Set<Integer> findExpiredIndexesBeforeIndex(int index) {
		long indexTime = getLineTime(index);
		Set<Integer> removeThese = Sets.newHashSet();
		for (int i = index - 1; i >= 0; i--) {
			if (indexTime - getLineTime(i) > MAXIMUM_ARRIVAL_TIME_DIFFERENCE_MS) {
				listener.timestampNotFound(getLine(i), getLineTime(i));
				removeThese.add(i);
			}
		}
		return removeThese;
	}

	/**
	 * Finds matches and reports them to the listeners if buffer has a sufficent
	 * span of time in it. Returns the indexes that should be removed from the
	 * buffer.
	 * 
	 * @param arrivalTime
	 * @param earliestTimestampLineIndex
	 * @return
	 */
	private Set<Integer> matchWithClosestAisMessageIfBufferLargeEnough(
			long arrivalTime, Integer earliestTimestampLineIndex) {
		String timestampLine = getLine(earliestTimestampLineIndex);
		long time = getLineTime(earliestTimestampLineIndex);
		log.debug("ts=" + timestampLine + ",time=" + time);
		Set<Integer> removeThese;
		if (arrivalTime - time > MAXIMUM_ARRIVAL_TIME_DIFFERENCE_MS) {
			removeThese = matchWithClosestAisMessageAndFindIndexesToRemove(
					earliestTimestampLineIndex, timestampLine, time);
		} else
			removeThese = findExpiredIndexesBeforeIndex(earliestTimestampLineIndex);
		return removeThese;
	}

	/**
	 * 
	 * Finds matches and reports them to the listeners. Returns the indexes that
	 * should be removed form the buffer.
	 * 
	 * @param earliestTimestampLineIndex
	 * @param timestampLine
	 * @param time
	 * @return
	 */
	private Set<Integer> matchWithClosestAisMessageAndFindIndexesToRemove(
			int earliestTimestampLineIndex, String timestampLine, long time) {
		// find closest ais message in terms of arrival time to the arrival
		// time of the timestamp line
		NmeaMessageExactEarthTimestamp timestamp = new NmeaMessageExactEarthTimestamp(
				timestampLine);
		String checksum = timestamp.getFollowingSequenceChecksum();
		log.debug("looking for checksum=" + checksum);

		Integer lowestTimeDiffIndex = findClosestMatchingMessageInTermsOfArrivalTime(
				time, checksum);
		Set<Integer> removeThese;
		if (lowestTimeDiffIndex != null) {
			removeThese = reportMatchAndFindIndexesToRemove(
					earliestTimestampLineIndex, timestamp, lowestTimeDiffIndex);
		} else
			// no match within BUFFER ms so remove
			removeThese = Sets.newHashSet(earliestTimestampLineIndex);
		return removeThese;
	}

	/**
	 * Returns the index of the closest matching message in terms of arrival
	 * time. Matching equates to having a referenced checksum. If not found
	 * returns null. If there is a problem parsing the message an
	 * {@link AisParseException} is thrown.
	 * 
	 * @param time
	 * @param checksum
	 * @return
	 */
	private Integer findClosestMatchingMessageInTermsOfArrivalTime(long time,
			String checksum) {
		// find the closest matching ais message in terms of arrival time
		Long lowestTimeDiff = null;
		Integer lowestTimeDiffIndex = null;
		for (int i = 0; i < getNumLines(); i++) {
			String msg = getLine(i);
			Long msgTime = getLineTime(i);
			if (!isExactEarthTimestamp(msg)) {
				try {
					AisNmeaMessage nmea = new AisNmeaMessage(msg);
					if (nmea.getChecksum().equals(checksum)) {
						long diff = Math.abs(msgTime - time);
						boolean closer = (lowestTimeDiff == null || diff < lowestTimeDiff)
								&& (diff <= MAXIMUM_ARRIVAL_TIME_DIFFERENCE_MS);
						if (closer) {
							lowestTimeDiff = diff;
							lowestTimeDiffIndex = i;
						}
					}
				} catch (AisParseException e) {
					log.debug(e.getMessage());
				}
			}
		}
		return lowestTimeDiffIndex;
	}

	/**
	 * Reports a found match and returns the indexes that can be removed from
	 * the buffer.
	 * 
	 * @param earliestTimestampLineIndex
	 * @param timestamp
	 * @param lowestTimeDiffIndex
	 * @return
	 */
	private Set<Integer> reportMatchAndFindIndexesToRemove(
			Integer earliestTimestampLineIndex,
			NmeaMessageExactEarthTimestamp timestamp,
			Integer lowestTimeDiffIndex) {
		String msg = getLine(lowestTimeDiffIndex);
		log.debug("found matching msg=" + msg);
		listener.message(msg, timestamp.getTime());
		int maxIndex = Math
				.max(lowestTimeDiffIndex, earliestTimestampLineIndex);
		int minIndex = Math
				.min(lowestTimeDiffIndex, earliestTimestampLineIndex);

		// now remove from lists must remove bigger index first,
		// otherwise indexes will change
		return Sets.newHashSet(minIndex, maxIndex);
	}

	/**
	 * Add a line to the buffer.
	 * 
	 * @param line
	 * @param time
	 */
	private void addLine(String line, long time) {
		lines.add(new LineAndTime(line, time));
	}

	/**
	 * Returns the buffer size.
	 * 
	 * @return
	 */
	private int getNumLines() {
		return lines.size();
	}

	/**
	 * Returns the buffer line at given index (zero based).
	 * 
	 * @param index
	 * @return
	 */
	private String getLine(int index) {
		return lines.get(index).getLine();
	}

	/**
	 * Returns the arrival time of line at given index (zero based).
	 * 
	 * @param index
	 * @return
	 */
	private long getLineTime(int index) {
		return lines.get(index).getTime();
	}

	/**
	 * Remove line from the buffer with given index (zero based).
	 * 
	 * @param index
	 */
	private void removeLineWithIndex(int index) {
		lines.remove(index);
	}

	/**
	 * Returns a copy of the buffer.
	 * 
	 * @return
	 */
	@VisibleForTesting
	List<LineAndTime> getBuffer() {
		return Lists.newArrayList(lines);
	}

	/**
	 * Returns the index of the earliest timestamp (PGHP) line. If none found
	 * returns null.
	 * 
	 * @param lines
	 * @return
	 */
	private static Integer getEarliestTimestampLineIndex(List<LineAndTime> lines) {
		Integer i = 0;
		for (LineAndTime line : lines) {
			if (isExactEarthTimestamp(line.getLine()))
				return i;
			else
				i++;
		}
		return null;
	}

}
