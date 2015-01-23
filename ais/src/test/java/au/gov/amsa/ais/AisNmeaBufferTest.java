package au.gov.amsa.ais;

import static au.gov.amsa.ais.AisNmeaBuffer.concatenateMessages;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaUtil;

public class AisNmeaBufferTest {

	@Test
	public void testTwoLinesAreBufferedAndAggregateIsReturned() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(5);
		checkTwoLinesAreBufferedAndAggregateIsReturned(buffer);
	}

	@Test
	public void testTwoLinesAreBufferedAndAggregateIsReturnedWhenOutOfOrder() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(5);
		checkTwoLinesAreBufferedAndAggregateIsReturnedWhenOutOfOrder(buffer);
	}

	private void checkTwoLinesAreBufferedAndAggregateIsReturned(
			AisNmeaBuffer buffer) {
		String line1 = "\\g:1-2-1130,c:1334278696*29\\!BSVDM,2,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh000,0*0B";
		String line2 = "\\g:2-2-1130*5E\\!BSVDM,2,2,0,A,00000000002,0*3D";
		assertFalse(buffer.add(NmeaUtil.parseNmea(line1)).isPresent());
		NmeaMessage m = AisNmeaBuffer.concatenateMessages(
				buffer.add(NmeaUtil.parseNmea(line2)).get()).get();
		// check that column 5 in line2 is appended to column 5 in line 1 and
		// line count and line number are 1 and tag block is as for line1.
		System.out.println(m.toLine());
		assertEquals(
				"\\g:1-1-1130,c:1334278696*2A\\!BSVDM,1,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh00000000000002,0*3A",
				m.toLine());
		System.out
				.println("msg="
						+ new AisMessageParser()
								.parse("55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh00000000000002"));
	}

	private void checkTwoLinesAreBufferedAndAggregateIsReturnedWhenOutOfOrder(
			AisNmeaBuffer buffer) {
		String line1 = "\\g:2-2-3987*58\\!BSVDM,2,2,0,A,lQ@@0000002,0*00";
		String line2 = "\\g:1-2-3987,c:1333239510*20\\!BSVDM,2,1,0,A,54`98002>?A1`<AGD00lEBr0PD5@PE:1<4hiT01CKh`IC4w8NKjCPj1Ck`2k,0*7C";
		assertFalse(buffer.add(NmeaUtil.parseNmea(line1)).isPresent());
		NmeaMessage m = concatenateMessages(
				buffer.add(NmeaUtil.parseNmea(line2)).get()).get();
		// check that column 5 in line2 is appended to column 5 in line 1 and
		// line count and line number are 1 and tag block is as for line1.
		System.out.println(m.toLine());
		assertEquals(
				"\\g:1-1-3987,c:1333239510*23\\!BSVDM,1,1,0,A,54`98002>?A1`<AGD00lEBr0PD5@PE:1<4hiT01CKh`IC4w8NKjCPj1Ck`2klQ@@0000002,0*70",
				m.toLine());
	}

	@Test
	public void testTwoLinesAreBufferedAndAggregateIsReturnedThenRepeatIsTreatedTheSame() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(10);
		checkTwoLinesAreBufferedAndAggregateIsReturned(buffer);
		checkTwoLinesAreBufferedAndAggregateIsReturned(buffer);
		checkTwoLinesAreBufferedAndAggregateIsReturned(buffer);
	}

	@Test
	public void checkBufferMaxSizeIsHonoured() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(2);
		// this message
		String line = "\\g:2-2-3985*5A\\!AIVDM,2,2,9,B,PFRC88888888880,2*29";
		assertFalse(buffer.add(NmeaUtil.parseNmea(line)).isPresent());
		assertEquals(1, buffer.size());
		String line2 = "\\g:2-2-1130*5E\\!BSVDM,2,2,0,A,00000000002,0*3D";
		assertFalse(buffer.add(NmeaUtil.parseNmea(line2)).isPresent());
		assertEquals(2, buffer.size());
		String line3 = "\\g:2-2-1132*5E\\!BSVDM,2,2,0,A,00000000002,0*3D";
		assertFalse(buffer.add(NmeaUtil.parseNmea(line3)).isPresent());
		assertEquals(2, buffer.size());
	}

	@Test
	public void checkLineWithTimestampButWithoutGroupingIsReturnedImmediately() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(2);
		// this message
		// note I edited the sentence count from 2 to 1 which may have stuffed
		// up checksum but still seems to process ?
		String line = "\\c:1334278696*29\\!BSVDM,1,1,0,A,55DSBL02<Dm7<I`OP005<T4r0hTiT00000000016=hJ<855f?>kV`54Qh000,0*0B";
		NmeaMessage m = NmeaUtil.parseNmea(line);
		assertEquals(m, concatenateMessages(buffer.add(m).get()).get());
	}

	@Test
	public void testLinewithTooFewColumnsIsReturnedImmediately() {
		AisNmeaBuffer buffer = new AisNmeaBuffer(5);
		NmeaMessage m = createMock(NmeaMessage.class);
		@SuppressWarnings("unchecked")
		List<String> items = createMock(ArrayList.class);
		expect(items.size()).andReturn(5).atLeastOnce();
		expect(m.getItems()).andReturn(items).atLeastOnce();
		replay(m, items);
		assertEquals(m, concatenateMessages(buffer.add(m).get()).get());
		verify(m, items);
	}

	@Test
	public void testBufferReturnsCorrectTimestamp() {
		String line = "\\s:rEV03,c:1334073836*52\\!AIVDM,1,1,,A,1770<U002V6pkcgmGalTw3sj1`QS,0*7E";
		AisNmeaBuffer buffer = new AisNmeaBuffer(50);
		NmeaMessage m = NmeaUtil.parseNmea(line);
		assertEquals(m, concatenateMessages(buffer.add(m).get()).get());
		AisNmeaMessage a = new AisNmeaMessage(m.toLine());
		assertEquals(1334073836000L, (long) a.getTime());
	}

}
