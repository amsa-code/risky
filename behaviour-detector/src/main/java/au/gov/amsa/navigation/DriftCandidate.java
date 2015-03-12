package au.gov.amsa.navigation;

import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;

public class DriftCandidate {

	private final Fix fix;
	private final long driftingSince;

	public DriftCandidate(Fix fix, long driftingSince) {
		this.fix = fix;
		this.driftingSince = driftingSince;
	}

	public Fix fix() {
		return fix;
	}

	public long driftingSince() {
		return driftingSince;
	}

	public static final Func1<DriftCandidate, Fix> TO_FIX = new Func1<DriftCandidate, Fix>() {
		@Override
		public Fix call(DriftCandidate c) {
			return c.fix();
		}
	};

	@Override
	public String toString() {
		return "DriftCandidate [driftingDurationMinutes=" + (fix.getTime() - driftingSince)
		        / 60000.0 + ", fix=" + fix + "]";
	}

}
