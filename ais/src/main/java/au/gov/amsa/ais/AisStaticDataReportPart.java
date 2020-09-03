package au.gov.amsa.ais;

import java.util.Optional;

public enum AisStaticDataReportPart {
	
	PART_A(0),
	PART_B(1);
	
	private final int partNumber;
	
	private AisStaticDataReportPart(int partNumberCode) {
		this.partNumber = partNumberCode;
	}
	
	public static Optional<AisStaticDataReportPart> lookup(int partNumberCode) {
		
		final AisStaticDataReportPart staticDataReportPart;
		
		if(partNumberCode == PART_A.getPartNumber()) {
			staticDataReportPart = PART_A;
		} else if (partNumberCode == PART_B.getPartNumber()) {
			staticDataReportPart = PART_B;
		} else {
			staticDataReportPart = null;
		}
		
		return Optional.ofNullable(staticDataReportPart);
	}
	
	public boolean isPartA() {
		return this==PART_A;
	}
	
	public boolean isPartB() {
		return this==PART_B;
	}

	public int getPartNumber() {
		return partNumber;
	}
}
