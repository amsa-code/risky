package au.gov.amsa.ais;

import au.gov.amsa.ais.message.AisPositionA;

/**
 * This factory creates {@link AisExtractor} instances. It is in use so that we
 * can mock AisExtractors used by the message parsers (for example
 * {@link AisPositionA}.
 * 
 * @author dxm
 * 
 */
public interface AisExtractorFactory {

	/**
	 * Returns an extractor for the given message. If the decoded message does
	 * not have the specified minimum length then an AisParseException is
	 * thrown.
	 * 
	 * @param message
	 * @param minLength
	 * @param padBits
	 * @return
	 */
	AisExtractor create(String message, int minLength, int padBits);
}
