package au.gov.amsa.ais;

import java.util.ResourceBundle;

/**
 * Handles resource lookup.
 * 
 * @author dxm
 * 
 */
public class Internationalization {

	private static final String BUNDLE_NAME = "au.gov.amsa.ais.messages";

	private static ResourceBundle bundle = null;

	/**
	 * Constructor.
	 */
	private Internationalization() {
	}

	/**
	 * Calls the private constructor to ensure 100% cobertura coverage.
	 */
	static void forTestCoverageOnly() {
		new Internationalization();
	}

	/**
	 * Returns the relevant {@link ResourceBundle} for the current locale.
	 * 
	 * @return
	 */
	private static synchronized ResourceBundle getBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(BUNDLE_NAME);
		return bundle;

	}

	/**
	 * Returns the value from the current {@link ResourceBundle} for the given
	 * key.
	 * 
	 * @param key
	 * @return
	 */
	public static String getString(String key) {

		return getBundle().getString(key);

	}
}
