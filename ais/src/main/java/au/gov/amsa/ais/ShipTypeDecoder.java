package au.gov.amsa.ais;

import java.math.BigInteger;

import com.google.common.annotations.VisibleForTesting;

/**
 * Decodes the ship type code as per 1371-4.pdf.
 * 
 * @author dxm
 * 
 */
public final class ShipTypeDecoder {

	/**
	 * Private constructor.
	 */
	private ShipTypeDecoder() {
		// private constructor.
	}

	/**
	 * Visible for test coverage only (calls the private constructor).
	 */
	@VisibleForTesting
	static void forTestCoverageOnly() {
		new ShipTypeDecoder();
	}

	private static String[] vesselTypes = { "Fishing", "Towing",
			"Towing Long/Large",
			"Engaged in dredging or underwater operations",
			"Engaged in diving operations", "Engaged in military operations",
			"Sailing", "Pleasure craft", "Reserved", "Reserved" };
	private static String[] specialTypes = { "Pilot vessel", "SAR", "Tug",
			"Port tender",
			"Vessel with anti-pollution facilities or equipment",
			"Law enforcement", "Local 56", "Local 57", "Medical transport",
			"Ship according to RR Resolution No. 18 (Mob-83)" };
	private static String[] categoryTypes = { "Unknown", "Reserved", "WIG",
			"Vessel", "HSC", "Special", "Passenger ship", "Cargo ship",
			"Tanker", "Other" };
	private static String[] cargoTypes = { "All",
			"Carrying DG, HS, or MP, IMO Hazard or pollutant category A",
			"Carrying DG, HS, or MP, IMO Hazard or pollutant category B",
			"Carrying DG, HS, or MP, IMO Hazard or pollutant category C",
			"Carrying DG, HS, or MP, IMO Hazard or pollutant category D",
			"Reserved 5", "Reserved 6", "Reserved 7", "Reserved 8",
			"No additional info" };

	/**
	 * Returns the ship type for given integer. If integer is null then returns
	 * null.
	 * 
	 * @param ts
	 * @return
	 */
	public static String getShipType(BigInteger ts) {
		if (ts == null)
			return null;
		else
			return getShipType(ts.intValue());
	}

	/**
	 * Returns the ship type for given integer. If integer is null then returns
	 * null.
	 * 
	 * @param ts
	 * @return
	 */
	public static String getShipType(Integer ts) {
		if (ts == null)
			return null;
		if (ts < 10 || ts > 99) {
			return "unknown code " + ts.intValue();
		} else {
			int a = ts / 10;
			int b = ts % 10;
			if (a == 3) {
				return vesselTypes[b];
			} else if (a == 5) {
				return specialTypes[b];
			} else {
				return categoryTypes[a] + " - " + cargoTypes[b];
			}
		}
	}

}
