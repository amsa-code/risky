package au.gov.amsa.util.identity;

import java.util.HashSet;
import java.util.Set;

public final class MmsiValidator2 {

	private final Set<Long> badIdentifiers = new HashSet<Long>();

	public static MmsiValidator2 INSTANCE = new MmsiValidator2();

	private MmsiValidator2() {
		badIdentifiers.add(123456789L);
		badIdentifiers.add(987654321L);
		badIdentifiers.add(111111111L);
		badIdentifiers.add(999999999L);
		// multiple ships use these mmsi numbers
		badIdentifiers.add(107374182L);
		badIdentifiers.add(503499100L);
		badIdentifiers.add(503000000L);
		badIdentifiers.add(777777777L);
		badIdentifiers.add(333333333L);
		badIdentifiers.add(525123456L);
		badIdentifiers.add(273000000L);
		badIdentifiers.add(525000000L);
		badIdentifiers.add(100000000L);
		badIdentifiers.add(1193046L);
	}

	/**
	 * Returns true if and only if the <code>mmsi</code> is a series of 9 digits
	 * and is not one of a set of bad identifiers e.g. 123456789.
	 * 
	 * @param mmsi
	 * @return
	 */
	public boolean isValid(long mmsi) {
		return mmsi <= 999999999L && mmsi >= 1000000L && !badIdentifiers.contains(mmsi);
	}
}
