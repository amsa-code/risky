package au.gov.amsa.navigation;

import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;

public class DriftCandidate implements HasFix {

	private final Fix fix;
	private final long driftingSince;

	public DriftCandidate(Fix fix, long driftingSince) {
		this.fix = fix;
		this.driftingSince = driftingSince;
	}

	@Override
	public Fix fix() {
		return fix;
	}

	public long driftingSince() {
		return driftingSince;
	}

	@Override
	public String toString() {
		return "DriftCandidate [driftingDurationMinutes=" + (fix.getTime() - driftingSince)
		        / 60000.0 + ", fix=" + fix + "]";
	}

}
