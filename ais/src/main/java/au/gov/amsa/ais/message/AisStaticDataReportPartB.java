package au.gov.amsa.ais.message;

import com.google.common.base.Optional;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.HasMmsi;

public interface AisStaticDataReportPartB extends AisMessage, HasMmsi {

	int getRepeatIndicator();
	
	int getShipType();
	
	String getVendorManufacturerId();
	
	Integer getVendorUnitModelCode();
	
	Integer getVendorUnitSerialNumber();
	
	String getCallsign();
	
	int getTypeOfElectronicPosition();
	
	Optional<Integer> getDimensionA();

	Optional<Integer> getDimensionB();

	Optional<Integer> getDimensionC();

	Optional<Integer> getDimensionD();

	Optional<Integer> getLengthMetres();

	Optional<Integer> getWidthMetres();
}