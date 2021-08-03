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

public class AisBStaticDataReportPartBTest {

	@Test
	public void testParser() {
		
		final String line = "H7PJ@:4UCBD6f6<57hhh001H5220";
		final String source = "some source";

		AisBStaticDataReportPartB message = new AisBStaticDataReportPartB(line, source, 0);

		assertEquals(24, message.getMessageId());
		assertEquals(503746600, message.getMmsi());
		assertEquals(0, message.getRepeatIndicator());
		assertEquals(AbstractAisBStaticDataReport.PART_NUMBER_B, message.getPartNumber());
		assertEquals(37, message.getShipType());
		assertEquals("SRT", message.getVendorManufacturerId());
		assertEquals(Integer.valueOf(1), message.getVendorUnitModelCode());
		assertEquals(Integer.valueOf(713100), message.getVendorUnitSerialNumber());
		assertEquals("EG000", message.getCallsign().get());
		assertEquals(Integer.valueOf(11), message.getDimensionA().get());
		assertEquals(Integer.valueOf(5), message.getDimensionB().get());
		assertEquals(Integer.valueOf(2), message.getDimensionC().get());
		assertEquals(Integer.valueOf(2), message.getDimensionD().get());
		assertEquals(source, message.getSource());
		assertEquals(Integer.valueOf(16), message.getLengthMetres().get());
		assertEquals(Integer.valueOf(4), message.getWidthMetres().get());
	}
	
	@Test
	public void testExtractPartNumber() {
		final String message = "H7PJ@:4UCBD6f6<57hhh001H5220";
		
		final int partNumber = AbstractAisBStaticDataReport.extractPartNumber(Util.getAisExtractorFactory(), message, 0);
		
		assertEquals(AbstractAisBStaticDataReport.PART_NUMBER_B, partNumber);
	}
	
	@Test
	public void testTypeOfShipAndCargoType() {
		final Integer typeOfShip = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(typeOfShip).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractShipType(ex);
		assertEquals(typeOfShip, result);
		verify(ex);
	}
	
	@Test
	public void testTypeOfShipAndCargoTypeNotAvailable() {
		
		final Integer typeOfShip = Integer.valueOf(0);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(typeOfShip).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractShipType(ex);
		assertEquals(0, result.intValue());
		verify(ex);
	}
	
	@Test
	public void testCallSignNotAvailable() {
		
		final String callSign = "@@@@@@@";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(callSign).atLeastOnce();
		replay(ex);
		Optional<String> result = AisBStaticDataReportPartB.extractCallSign(ex);
		assertFalse(result.isPresent());
		verify(ex);
	}
	
	@Test
	public void testVendorManufacturerId() {
		final String vendorManufacturerId = "ABC";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(vendorManufacturerId).atLeastOnce();
		replay(ex);
		String result = AisBStaticDataReportPartB.extractVendorManufacturerId(ex);
		assertEquals(vendorManufacturerId, result);
		verify(ex);
	}
	
	@Test
	public void testVendorUnitModelCode() {
		final Integer vendorUnitModelCode = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(vendorUnitModelCode).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractVendorUnitModelCode(ex);
		assertEquals(vendorUnitModelCode, result);
		verify(ex);
	}
	
	@Test
	public void testVendorUnitSerialNumber() {
		final Integer vendorUnitSerialNumber = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(vendorUnitSerialNumber).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractVendorUnitSerialNumber(ex);
		assertEquals(vendorUnitSerialNumber, result);
		verify(ex);
	}
	
	@Test
	public void testCallSign() {
		final String vendorCallSign = "ABC";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(vendorCallSign).atLeastOnce();
		replay(ex);
		Optional<String> result = AisBStaticDataReportPartB.extractCallSign(ex);
		assertEquals(vendorCallSign, result.get());
		verify(ex);
	}
	
	@Test
	public void testDimensionA() {
		final Integer dimensionA = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionA).atLeastOnce();
		replay(ex);
		Optional<Integer> result = AisBStaticDataReportPartB.extractDimensionA(ex);
		assertEquals(dimensionA, result.get());
		verify(ex);
	}
	
	@Test
	public void testDimensionB() {
		final Integer dimensionB = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionB).atLeastOnce();
		replay(ex);
		Optional<Integer> result = AisBStaticDataReportPartB.extractDimensionB(ex);
		assertEquals(dimensionB, result.get());
		verify(ex);
	}
	
	@Test
	public void testDimensionC() {
		final Integer dimensionC = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionC).atLeastOnce();
		replay(ex);
		Optional<Integer> result = AisBStaticDataReportPartB.extractDimensionC(ex);
		assertEquals(dimensionC, result.get());
		verify(ex);
	}
	
	@Test
	public void testDimensionD() {
		final Integer dimensionD = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionD).atLeastOnce();
		replay(ex);
		Optional<Integer> result = AisBStaticDataReportPartB.extractDimensionD(ex);
		assertEquals(dimensionD, result.get());
		verify(ex);
	}
}
