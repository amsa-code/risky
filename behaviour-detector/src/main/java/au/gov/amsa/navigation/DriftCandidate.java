package au.gov.amsa.navigation;

import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;

public class DriftCandidate implements HasFix {

    private final HasFix fix;
    private final long driftingSince;

    public DriftCandidate(HasFix fix, long driftingSince) {
        this.fix = fix;
        this.driftingSince = driftingSince;
    }

    public HasFix fixWwrapper() {
        return fix;
    }

    @Override
    public Fix fix() {
        return fix.fix();
    }

    public long driftingSince() {
        return driftingSince;
    }

    @Override
    public String toString() {
        return "DriftCandidate [driftingDurationMinutes=" + (fix.fix().time() - driftingSince)
                / 60000.0 + ", fix=" + fix + "]";
    }

}
