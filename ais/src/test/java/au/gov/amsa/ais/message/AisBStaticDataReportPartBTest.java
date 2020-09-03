package au.gov.amsa.ais.message;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import au.gov.amsa.ais.AisExtractor;
import au.gov.amsa.ais.AisStaticDataReportPart;
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
		assertEquals(AisStaticDataReportPart.PART_B.getPartNumber(), message.getPartNumber());
		assertEquals(37, message.getShipType());
		assertEquals("SRT", message.getVendorManufacturerId());
		assertEquals(Integer.valueOf(1), message.getVendorUnitModelCode());
		assertEquals(Integer.valueOf(713100), message.getVendorUnitSerialNumber());
		assertEquals("EG000", message.getCallsign());
		assertEquals(Integer.valueOf(11), message.getDimensionA().get());
		assertEquals(Integer.valueOf(5), message.getDimensionB().get());
		assertEquals(Integer.valueOf(2), message.getDimensionC().get());
		assertEquals(Integer.valueOf(2), message.getDimensionD().get());
		assertEquals(0, message.getTypeOfElectronicPosition());
		assertEquals(source, message.getSource());
		assertEquals(Integer.valueOf(16), message.getLengthMetres().get());
		assertEquals(Integer.valueOf(4), message.getWidthMetres().get());
	}
	
	@Test
	public void testExtractPartNumber() {
		final String message = "H7PJ@:4UCBD6f6<57hhh001H5220";
		
		final int partNumber = AbstractAisBStaticDataReport.extractPartNumber(Util.getAisExtractorFactory(), message, 0);
		
		assertEquals(AisStaticDataReportPart.PART_B.getPartNumber(), partNumber);
	}
	
	@Test
	public void testTypeOfShipAndCargoType() {
		final Integer typeOfShip = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(typeOfShip).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractShipType(ex);
		assertEquals(typeOfShip, result);
	}
	
	@Test
	public void testTypeOfShipAndCargoTypeNotAvailable() {
		
		final Integer typeOfShip = Integer.valueOf(0);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(typeOfShip).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractShipType(ex);
		assertNull(result);
	}
	
	@Test
	public void testCallSignNotAvailable() {
		
		final String callSign = "@@@@@@@";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(callSign).atLeastOnce();
		replay(ex);
		String result = AisBStaticDataReportPartB.extractCallSign(ex);
		assertNull(result);
	}
	
	@Test
	public void testVendorManufacturerId() {
		final String vendorManufacturerId = "ABC";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(vendorManufacturerId).atLeastOnce();
		replay(ex);
		String result = AisBStaticDataReportPartB.extractVendorManufacturerId(ex);
		assertEquals(vendorManufacturerId, result);
	}
	
	@Test
	public void testVendorUnitModelCode() {
		final Integer vendorUnitModelCode = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(vendorUnitModelCode).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractVendorUnitModelCode(ex);
		assertEquals(vendorUnitModelCode, result);
	}
	
	@Test
	public void testVendorUnitSerialNumber() {
		final Integer vendorUnitSerialNumber = Integer.valueOf(123);
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(vendorUnitSerialNumber).atLeastOnce();
		replay(ex);
		Integer result = AisBStaticDataReportPartB.extractVendorUnitSerialNumber(ex);
		assertEquals(vendorUnitSerialNumber, result);
	}
	
	@Test
	public void testCallSign() {
		final String vendorCallSign = "ABC";
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getString(anyInt(), anyInt())).andReturn(vendorCallSign).atLeastOnce();
		replay(ex);
		String result = AisBStaticDataReportPartB.extractCallSign(ex);
		assertEquals(vendorCallSign, result);
	}
	
	@Test
	public void testDimensionA() {
		final int dimensionA = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionA).atLeastOnce();
		replay(ex);
		int result = AisBStaticDataReportPartB.extractDimensionA(ex);
		assertEquals(dimensionA, result);
	}
	
	@Test
	public void testDimensionB() {
		final int dimensionB = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionB).atLeastOnce();
		replay(ex);
		int result = AisBStaticDataReportPartB.extractDimensionB(ex);
		assertEquals(dimensionB, result);
	}
	
	@Test
	public void testDimensionC() {
		final int dimensionC = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionC).atLeastOnce();
		replay(ex);
		int result = AisBStaticDataReportPartB.extractDimensionC(ex);
		assertEquals(dimensionC, result);
	}
	
	@Test
	public void testDimensionD() {
		final int dimensionD = 123;
		
		AisExtractor ex = createMock(AisExtractor.class);
		expect(ex.getValue(anyInt(), anyInt())).andReturn(dimensionD).atLeastOnce();
		replay(ex);
		int result = AisBStaticDataReportPartB.extractDimensionD(ex);
		assertEquals(dimensionD, result);
	}
}
