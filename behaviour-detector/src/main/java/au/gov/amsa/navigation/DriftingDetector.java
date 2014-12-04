package au.gov.amsa.navigation;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Preconditions;

public class DriftingDetector {

	private static final int HEADING_COG_DIFFERENCE_MIN = 70;
	private static final int HEADING_COG_DIFFERENCE_MAX = 110;
	protected static final double MAX_DRIFTING_SPEED_KNOTS = 4;
	protected static final double MIN_DRIFTING_SPEED_KNOTS = 0.3;

	public Observable<VesselPosition> getCandidates(Observable<VesselPosition> o) {
		return o.filter(isCandidate());
	}

	private Func1<VesselPosition, Boolean> isCandidate() {

		return new Func1<VesselPosition, Boolean>() {

			@Override
			public Boolean call(VesselPosition p) {
				if (p.cogDegrees().isPresent()
						&& p.headingDegrees().isPresent()) {
					double diff = diff(p.cogDegrees().get(), p.headingDegrees()
							.get());
					return diff >= HEADING_COG_DIFFERENCE_MIN
							&& diff <= HEADING_COG_DIFFERENCE_MAX
							&& p.speedMetresPerSecond().get() <= MAX_DRIFTING_SPEED_KNOTS * 0.5144444
							&& p.speedMetresPerSecond().get() > MIN_DRIFTING_SPEED_KNOTS;
				} else
					return false;
			}
		};
	}

	static double diff(double a, double b) {
		Preconditions.checkArgument(a >= 0 && a < 360,
				"must be between 0 and 360" + a);
		Preconditions.checkArgument(b >= 0 && b < 360,
				"must be between 0 and 360: " + b);
		double value;
		if (a < b)
			value = a + 360 - b;
		else
			value = a - b;
		if (value > 180)
			return 360 - value;
		else
			return value;

	};

}
