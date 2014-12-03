package au.gov.amsa.ais;

/**
 * Used to indicate that an AIS message contains a Communications part.
 * 
 * @author dxm
 * 
 */
public interface HasCommunications {

	/**
	 * Returns the communications part of an AIS Message.
	 * 
	 * @return
	 */
	Communications getCommunications();

}
