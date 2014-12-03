package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.HasMmsi;

public interface AisPosition extends AisMessage, HasMmsi {

	int getRepeatIndicator();

	Double getSpeedOverGroundKnots();

	boolean isHighAccuracyPosition();

	Double getLongitude();

	Double getLatitude();

	Double getCourseOverGround();

	Integer getTrueHeading();

	int getTimeSecondsOnly();

	boolean isUsingRAIM();

}