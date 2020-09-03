package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.HasMmsi;

public interface AisStaticDataReport extends AisMessage, HasMmsi {

	int getRepeatIndicator();

	int getPartNumber();
}