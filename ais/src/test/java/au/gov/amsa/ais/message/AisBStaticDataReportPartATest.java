package au.gov.amsa.ais.message;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.Util;

public class AisBStaticDataReportPartATest {

	@Test
	public void testParser() {

		final String line = "H7PH6A15DDDr1=@5:0f22222220";
		final String source = "some source";
		
		AisBStaticDataReportPartA message = new AisBStaticDataReportPartA(line, source, 0);

		assertEquals(24, message.getMessageId());
		assertEquals(503711300, message.getMmsi());
		assertEquals(0, message.getRepeatIndicator());
		assertEquals(0, message.getPartNumber());
		assertEquals("QUEEN STAR K", message.getName().get());
		assertEquals(source, message.getSource());
	}
	
	@Test
	public void testExtractPartNumber() {
		final String message = "H7PH6A15DDDr1=@5:0f22222220";
		
		final int partNumber = AbstractAisBStaticDataReport.extractPartNumber(Util.getAisExtractorFactory(), message, 0);
		
		assertEquals(0, partNumber);
	}

	@Test
	public void testName() {
		
		final String shipName = "Ship name";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(shipName).atLeastOnce();
		replay(ex);
		Optional<String> name = AisBStaticDataReportPartA.extractName(ex);
		assertEquals(shipName, name.get());
		verify(ex);
	}
	
	@Test
	public void testNameNotAvailable() {
		
		final String shipName = "@@@@@@@@@@@@@@@@@@@@";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(shipName).atLeastOnce();
		replay(ex);
		Optional<String> name = AisBStaticDataReportPartA.extractName(ex);
		assertFalse(name.isPresent());
		verify(ex);		
	}
}
