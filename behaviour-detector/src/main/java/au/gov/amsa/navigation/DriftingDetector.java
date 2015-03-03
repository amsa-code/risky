package au.gov.amsa.navigation;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class DriftingDetector {

	static final double KNOTS_TO_METRES_PER_SECOND = 0.5144444;
	@VisibleForTesting
	static final int HEADING_COG_DIFFERENCE_MIN = 70;
	@VisibleForTesting
	static final int HEADING_COG_DIFFERENCE_MAX = 110;
	@VisibleForTesting
	static final double MAX_DRIFTING_SPEED_KNOTS = 4;
	@VisibleForTesting
	static final double MIN_DRIFTING_SPEED_KNOTS = 0.3;

	public Observable<VesselPosition> getCandidates(Observable<VesselPosition> o) {
		return o.filter(IS_CANDIDATE);
	}

	private static class DriftingTransformer implements Transformer<VesselPosition, VesselPosition> {

		private DriftingDetector d = new DriftingDetector();

		@Override
		public Observable<VesselPosition> call(Observable<VesselPosition> o) {
			return d.getCandidates(o);
		}
	}

	public static DriftingTransformer detectDrift() {
		return new DriftingTransformer();
	}

	@VisibleForTesting
	static Func1<VesselPosition, Boolean> IS_CANDIDATE = new Func1<VesselPosition, Boolean>() {

		@Override
		public Boolean call(VesselPosition p) {
			if (p.cogDegrees().isPresent() && p.headingDegrees().isPresent()
			        && p.speedMetresPerSecond().isPresent()) {
				double diff = diff(p.cogDegrees().get(), p.headingDegrees().get());
				return diff >= HEADING_COG_DIFFERENCE_MIN
				        && diff <= HEADING_COG_DIFFERENCE_MAX
				        && p.speedMetresPerSecond().get() <= MAX_DRIFTING_SPEED_KNOTS
				                * KNOTS_TO_METRES_PER_SECOND
				        && p.speedMetresPerSecond().get() > MIN_DRIFTING_SPEED_KNOTS
				                * KNOTS_TO_METRES_PER_SECOND;
			} else
				return false;
		}
	};

	static double diff(double a, double b) {
		Preconditions.checkArgument(a >= 0 && a < 360, "must be between 0 and 360" + a);
		Preconditions.checkArgument(b >= 0 && b < 360, "must be between 0 and 360: " + b);
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
