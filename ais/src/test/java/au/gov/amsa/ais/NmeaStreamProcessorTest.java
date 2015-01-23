package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import au.gov.amsa.util.nmea.NmeaReader;
import au.gov.amsa.util.nmea.NmeaReaderFromSocket;

import com.google.common.collect.Lists;

public class NmeaStreamProcessorTest {

	@Test
	public void testStreamHasNoTimestampsAtStart() {
		NmeaStreamProcessor p = new NmeaStreamProcessor(
				TstUtil.createLoggingListener(System.out), true);
		p.line("!BSVDM,1,1,0,1,53F=qV02>n<<D<`H001A8tpTt00000000000000N3`J384V<t=PhD1H53mkP00000000000,2*67",
				1328133720012L);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22",
				1328133720087L);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B",
				1328133720089L);
		p.line("!BSVDM,1,1,0,1,544gjIP2209mD<e`0004d4@DlTf0HD@u8uH0001J8Q9D44a8tE0hD1H53mkP00000000000,2*75",
				1328133720096L);
		p.line("!AIVDM,1,1,,A,13F=qV8P1L5GG8QK6<JKHOv02HRu,0*6D",
				1328133720097L);

		System.out.println(System.currentTimeMillis());

	}

	@Test
	public void testRemovingExpiredLines() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 0);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22",
				1328133720087L);
		// first line will have expired
		assertEquals(1, p.getBuffer().size());
		assertEquals(0, recorder.getTimestampsFound().size());
	}

	@Test
	public void testMatchIfBufferTimeBigEnough() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 0);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22",
				1328133720087L);
		// first line will have expired
		assertEquals(1, p.getBuffer().size());
		assertEquals(0, recorder.getTimestampsFound().size());
	}

	@Test
	public void testMultipleChecksumMatchesFoundToTimestamp() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		// message go in a millisecond apart
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 1);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 2);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 3);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 4);
		// much later to expire the buffer
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B",
				111111111111111L);
		// first line will have expired
		assertEquals(3, p.getBuffer().size());
		assertEquals(1, recorder.getTimestampsFound().size());
	}

	@Test
	public void testLogLineGetsCalled() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		// pass 1 as log count frequency so every line gets a log call
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true, 1);
		// message go in a millisecond apart
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 0);
	}

	@Test
	public void testMultipleChecksumMatchesFoundToTimestampLaterMessageCloser() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		// message go in a millisecond apart
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 0);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 3);
		p.line("!AIVDM,1,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22", 4);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 4);
		// much later to expire the buffer
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B",
				111111111111111L);
		p.line("!AIVDM,1,1,,A,13F=qV8P1L5GG8QK6<JKHOv02HRu,0*6D",
				111111111111111L);
		// first line will have expired
		assertEquals(2, p.getBuffer().size());
		assertEquals(2, recorder.getTimestampsFound().size());
	}

	@Test
	public void testInvalidPghpLineCannotBeParsedAsAisNmea() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line("$PGHP,1,2012,2,1,20,19,5,651,316,2,316000002,1AIS_S,22*4B", 4);
		p.line("$PGHP,2,1,1,,02011E06035549440600060453554944060006045555494406267B42373135393341332D334145432D344141372D394436352D3037433941344138464644377D0603554E410618637573745F65655F696E7465726E616C5F646973706C61790603414354020104*68");
	}

	@Test
	public void testStreamDoesNotThrowExceptionWhenTimestampLineDoesNotHaveEnoughFields() {
		String line = "$PGHP,1,2012,22*4B";
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line(line);
		assertEquals(0, p.getBuffer().size());
		assertEquals(0, recorder.getTimestampsFound().size());
		assertEquals(0, recorder.getTimestampNotFound().size());
	}

	@Test
	public void testStreamReportsNmeaLineWithoutBufferingIfMatchWithTimestampLineIsFalse() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, false);
		String line = "!AIVDM,1,1,,A,13F=qV8P1L5GG8QK6<JKHOv02HRu,0*6D";
		long time = 1328133720097000L;
		p.line(line, time);
		assertEquals(0, p.getBuffer().size());
		assertEquals(1, recorder.getTimestampsFound().size());
		assertEquals(line, recorder.getTimestampsFound().get(0).getLeft());
		assertEquals(time, (long) recorder.getTimestampsFound().get(0)
				.getRight());
	}

	@Test
	public void testStreamProcessorWithInvalidNmeaLine() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line("abc");
		assertEquals(0, p.getBuffer().size());
		assertEquals(0, recorder.getTimestampsFound().size());
		assertEquals(0, recorder.getTimestampNotFound().size());
	}

	@Test
	public void testLineWithTimestamp() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		String line = "\\c:1334278696*29\\!BSVDM,2,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh000,0*0B";
		p.line(line, 1);
	}

	@Test
	public void testLineWithTimestampThatIsBuffered() {
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		String line = "\\g:1-2-1130,c:1334278696*29\\!BSVDM,2,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh000,0*0B";
		p.line(line, 1);
	}

	@Test
	public void testFullMessageGroupWithTimestampOnFirstLineIsReturnedAggregated() {
		String line1 = "\\g:1-2-1130,c:1334278696*29\\!BSVDM,2,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh000,0*0B";
		String line2 = "\\g:2-2-1130*5E\\!BSVDM,2,2,0,A,00000000002,0*3D";
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line(line1, 1);
		p.line(line2, 2);
		assertEquals(1, recorder.getTimestampsFound().size());
		assertEquals(1334278696000L, (long) recorder.getTimestampsFound()
				.get(0).getRight());
	}

	@Test
	public void testMultiSequenceMessageWithoutTagBlock() {

		String line1 = "!BSVDM,2,1,6,B,53m`5t02?whlh4`ON21=@5H4pLE:08Dhj222221@HhOJ@5W90LiiAC3kQp88,0*70";
		String line2 = "!BSVDM,2,2,6,B,88888888880,2*38";
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line(line1, 1);
		p.line(line2, 23);
		assertEquals(1, recorder.getTimestampsFound().size());
		System.out.println(recorder.getTimestampsFound());
		assertEquals(23L, (long) recorder.getTimestampsFound().get(0)
				.getRight());
	}

	@Test
	public void testNullPointerExceptionDoesNotOccur() {
		String line1 = "\\g:1-2-1947*55\\!BSVDM,2,1,0,A,57ldWV3wwwwt000000000000000000000000003w0000000Ht00000000000,0*38";
		String line2 = "\\g:2-2-1947*56\\!BSVDM,2,2,0,A,00000000002,0*3D";
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line(line1, 1);
		p.line(line2, 2);
	}

	@Test
	public void testNotEnoughPartsInGTagErrorInSecondLineOfMultilineMessageDoesNotThrowException() {
		String line1 = "!AIVDM,2,1,,A,144gjIP0085KKPQHMPjT81Sb1PSL,0*22";
		String line2 = "!ABVDM,2,2,,,88888888800000000000000000000,4*50";
		NmeaStreamProcessorListenerRecording recorder = new NmeaStreamProcessorListenerRecording();
		NmeaStreamProcessor p = new NmeaStreamProcessor(recorder, true);
		p.line(line1, 1);
		p.line(line2, 2);
	}

	private static class NmeaStreamProcessorListenerRecording implements
			NmeaStreamProcessorListener {

		private final List<Pair<String, Long>> list = Lists.newArrayList();
		private final List<Pair<String, Long>> notFound = Lists.newArrayList();
		private final List<Pair<String, Long>> invalidNmea = Lists
				.newArrayList();

		@Override
		public void message(String line, long time) {
			list.add(Pair.of(line, time));
		}

		@Override
		public void timestampNotFound(String line, Long arrivalTime) {
			notFound.add(Pair.of(line, arrivalTime));
		}

		public List<Pair<String, Long>> getTimestampsFound() {
			return list;
		}

		public List<Pair<String, Long>> getTimestampNotFound() {
			return notFound;
		}

		@Override
		public void invalidNmea(String line, long arrivalTime, String message) {
			invalidNmea.add(Pair.of(line, arrivalTime));
		}

	}

	public static void main(String[] args) {
		NmeaReader con = new NmeaReaderFromSocket("cbrais01.amsa.gov.au", 1235);
		TstUtil.process(con, TstUtil.createLoggingListener(System.out),
				System.out);
	}

}
