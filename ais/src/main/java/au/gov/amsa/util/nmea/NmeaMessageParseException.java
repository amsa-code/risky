package au.gov.amsa.util.nmea;

/**
 * Exception for use by {@link NmeaMessageParser}.
 * 
 * @author dxm
 * 
 */
public class NmeaMessageParseException extends RuntimeException {

	private static final long serialVersionUID = -971758317971727843L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 */
	public NmeaMessageParseException(String message) {
		super(message);
	}

	public NmeaMessageParseException(Throwable t) {
		super(t);
	}

	public NmeaMessageParseException(String message, Throwable t) {
		super(message, t);
	}

}
