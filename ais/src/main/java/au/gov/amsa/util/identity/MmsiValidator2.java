package au.gov.amsa.util.identity;

import java.util.HashSet;
import java.util.Set;

public final class MmsiValidator2 {

    private final Set<Long> bad = new HashSet<Long>();

    public static MmsiValidator2 INSTANCE = new MmsiValidator2();

    private MmsiValidator2() {
        bad.add(123456789L);
        bad.add(987654321L);
        bad.add(111111111L);
        bad.add(999999999L);
        // multiple ships use these mmsi numbers
        bad.add(107374182L);
        bad.add(503499100L);
        bad.add(503000000L);
        bad.add(777777777L);
        bad.add(333333333L);
        bad.add(525123456L);
        bad.add(273000000L);
        bad.add(525000000L);
        bad.add(100000000L);
        bad.add(553111692L);
        bad.add(888888888L);
        bad.add(555555555L);
        bad.add(273000000L);
        bad.add(1193046L);
        bad.add(222222222L);
        bad.add(352286000L);
        bad.add(352055000L);
    }

    /**
     * Returns true if and only if the <code>mmsi</code> is a series of 9 digits
     * and is not one of a set of bad identifiers e.g. 123456789.
     * 
     * @param mmsi
     * @return
     */
    public boolean isValid(long mmsi) {
        return mmsi <= 999999999L && mmsi >= 1000000L && !bad.contains(mmsi);
    }
}
