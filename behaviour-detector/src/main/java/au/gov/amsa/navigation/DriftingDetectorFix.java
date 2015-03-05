package au.gov.amsa.navigation;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class DriftingDetectorFix {

	static final double KNOTS_TO_METRES_PER_SECOND = 0.5144444;
	@VisibleForTesting
	static final int HEADING_COG_DIFFERENCE_MIN = 70;
	@VisibleForTesting
	static final int HEADING_COG_DIFFERENCE_MAX = 110;
	@VisibleForTesting
	static final double MAX_DRIFTING_SPEED_KNOTS = 4;
	@VisibleForTesting
	static final double MIN_DRIFTING_SPEED_KNOTS = 0.3;

	public Observable<Fix> getCandidates(Observable<Fix> o) {
		return o.filter(IS_CANDIDATE);
	}

	public static DriftingTransformer detectDrift() {
		return new DriftingTransformer();
	}

	private static class DriftingTransformer implements Transformer<Fix, Fix> {

		private DriftingDetectorFix d = new DriftingDetectorFix();

		@Override
		public Observable<Fix> call(Observable<Fix> o) {
			return d.getCandidates(o);
		}
	}

	@VisibleForTesting
	static Func1<Fix, Boolean> IS_CANDIDATE = new Func1<Fix, Boolean>() {

		@Override
		public Boolean call(Fix p) {
			if (p.getCourseOverGroundDegrees().isPresent()
			        && p.getHeadingDegrees().isPresent()
			        && p.getSpeedOverGroundKnots().isPresent()
			        && (!p.getNavigationalStatus().isPresent() || (p.getNavigationalStatus().get() != NavigationalStatus.AT_ANCHOR && p
			                .getNavigationalStatus().get() != NavigationalStatus.MOORED))) {
				double diff = diff(p.getCourseOverGroundDegrees().get(), p.getHeadingDegrees()
				        .get());
				return diff >= HEADING_COG_DIFFERENCE_MIN && diff <= HEADING_COG_DIFFERENCE_MAX
				        && p.getSpeedOverGroundKnots().get() <= MAX_DRIFTING_SPEED_KNOTS

				        && p.getSpeedOverGroundKnots().get() > MIN_DRIFTING_SPEED_KNOTS;
			} else
				return false;
		}
	};

	static double diff(double a, double b) {
		Preconditions.checkArgument(a >= 0 && a < 360);
		Preconditions.checkArgument(b >= 0 && b < 360);
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
