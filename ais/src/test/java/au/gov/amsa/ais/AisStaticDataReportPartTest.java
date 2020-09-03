package au.gov.amsa.ais;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class AisStaticDataReportPartTest {

	@Test
	public void testLookupPartFound() {
		
		for(AisStaticDataReportPart part : AisStaticDataReportPart.values()) {
			Optional<AisStaticDataReportPart> result =
					AisStaticDataReportPart.lookup(part.getPartNumber());
			
			Assert.assertEquals("Check part A is found", part, result.get());	
		}
	}
	
	@Test
	public void testLookupPartNotFound() {
		Optional<AisStaticDataReportPart> result =
				AisStaticDataReportPart.lookup(-1);
		
		Assert.assertFalse("Check no AisStaticDataReportPart is not found", result.isPresent());		
	}
	
	@Test
	public void testIsPartA() {
		Assert.assertTrue("Check the AisStaticDataReportPart is part A", AisStaticDataReportPart.PART_A.isPartA());
	}
	
	@Test
	public void testIsPartB() {
		Assert.assertTrue("Check the AisStaticDataReportPart is part B", AisStaticDataReportPart.PART_B.isPartB());
	}
	
	@Test
	public void testIsNotPartA() {
		Assert.assertFalse("Check the AisStaticDataReportPart is not part A", AisStaticDataReportPart.PART_B.isPartA());
	}
	
	@Test
	public void testIsNotPartB() {
		Assert.assertFalse("Check the AisStaticDataReportPart is not part B", AisStaticDataReportPart.PART_A.isPartB());
	}
}
