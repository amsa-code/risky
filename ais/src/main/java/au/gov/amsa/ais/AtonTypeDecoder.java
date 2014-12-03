package au.gov.amsa.ais;

import java.math.BigInteger;

import com.google.common.annotations.VisibleForTesting;

/**
 * Decodes the AtoN type code as per Table 71 in 1371-4.pdf.
 * 
 * @author pxg
 * 
 */
public final class AtonTypeDecoder {

	/**
	 * Private constructor.
	 */
	private AtonTypeDecoder() {
		// private constructor.
	}

	/**
	 * Visible for test coverage only (calls the private constructor).
	 */
	@VisibleForTesting
	static void forTestCoverageOnly() {
		new AtonTypeDecoder();
	}

	private static String[] categoryTypes = { "Fixed - ", "Floating - " };

	private static String[] atonTypes = { "Not specified", "Reference point",
			"RACON", "Fixed structure", "Reserved for future use",
			"Light, without sectors", "Light, with sectors",
			"Leading Light Front", "Leading Light Rear", "Beacon, Cardinal N",
			"Beacon, Cardinal E", "Beacon, Cardinal S", "Beacon, Cardinal W",
			"Beacon, Port hand", "Beacon, Starboard hand",
			"Beacon, Preferred Channel port hand",
			"Beacon, Preferred Channel starboard hand",
			"Beacon, Isolated danger", "Beacon, Safe water",
			"Beacon, Special mark", "Cardinal Mark N", "Cardinal Mark E",
			"Cardinal Mark S", "Cardinal Mark W", "Port hand Mark",
			"Starboard hand Mark", "Preferred Channel Port hand",
			"Preferred Channel Starboard hand", "Isolated danger",
			"Safe Water", "Special Mark", "Light Vessel, LANBY, Rigs" };

	/**
	 * Returns the AtoN type for given integer. If integer is null then returns
	 * null.
	 * 
	 * @param ts
	 * @return
	 */
	public static String getAtonType(BigInteger ts) {
		if (ts == null)
			return null;
		else
			return getAtonType(ts.intValue());
	}

	/**
	 * Returns the AtoN type for given integer. If integer is null then returns
	 * null.
	 * 
	 * @param ts
	 * @return
	 */
	public static String getAtonType(Integer ts) {
		if (ts == null)
			return null;
		if (ts < 0 || ts > 31) {
			return "unknown code " + ts.intValue();
		} else {
			if (ts >= 0 && ts < 5) {
				return atonTypes[ts];
			} else if (ts < 20) {
				return categoryTypes[0] + atonTypes[ts];
			} else {
				return categoryTypes[1] + atonTypes[ts];
			}
		}
	}

}
