package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.HasMmsi;

import com.google.common.base.Optional;

public interface AisShipStatic extends AisMessage, HasMmsi {

	int getRepeatIndicator();

	String getName();

	Optional<Integer> getDimensionA();

	Optional<Integer> getDimensionB();

	Optional<Integer> getDimensionC();

	Optional<Integer> getDimensionD();

	Optional<Integer> getLengthMetres();

	Optional<Integer> getWidthMetres();

	int getShipType();
	
	

}
