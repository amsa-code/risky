package au.gov.amsa.ais;

import au.gov.amsa.ais.message.AisAidToNavigation;
import au.gov.amsa.ais.message.AisBaseStation;
import au.gov.amsa.ais.message.AisMessageOther;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisPositionB;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;

/**
 * Parses AIS messages (as they are taken from the 5th column in the NMEA
 * message).
 * 
 * @author dxm
 * 
 */
public class AisMessageParser {

	private final AisExtractorFactory factory;

	/**
	 * Constructor.
	 */
	public AisMessageParser() {
		this(Util.getAisExtractorFactory());
	}

	/**
	 * Constructor.
	 * 
	 * @param factory
	 */
	public AisMessageParser(AisExtractorFactory factory) {
		this.factory = factory;
	}

	/**
	 * Returns an {@link AisMessage} from the string representation of the
	 * message as per 1371-4 IMO specification (as per the appropriate column in
	 * the NMEA message). Sets source to null.
	 * 
	 * @param message
	 * @return
	 */
	public AisMessage parse(String message, int padBits) {
		return parse(message, null, padBits);
	}

	/**
	 * Returns an {@link AisMessage} from the string representation of the
	 * message as per 1371-4 IMO specification (as per the appropriate column in
	 * the NMEA message).
	 * 
	 * @param message
	 * @param source
	 * @return
	 */
	public AisMessage parse(String message, String source, int padBits) {
		AisExtractor extractor = factory.create(message, 0, padBits);
		int id = extractor.getMessageId();
		if (Util.isClassAPositionReport(id)) {
			return new AisPositionA(message, source, padBits);
		} else if (id == 4)
			return new AisBaseStation(message, source, padBits);
		else if (id == 5)
			return new AisShipStaticA(message, source, padBits);
		else if (id == 18)
			return new AisPositionB(message, source, padBits);
		else if (id == 19)
			return new AisPositionBExtended(message, source, padBits);
		else if (id == 21)
			return new AisAidToNavigation(message, source, padBits);
		else
			return new AisMessageOther(id, source, padBits);
	}

}
