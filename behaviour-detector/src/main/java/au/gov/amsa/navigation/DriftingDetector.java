package au.gov.amsa.navigation;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import au.gov.amsa.risky.format.HasFix;

public class DriftingDetector {

    public Observable<DriftCandidate> getCandidates(Observable<HasFix> o) {
        return o.lift(detectDriftCandidates());
    }

    /**
     * This operator expects a stream of fixes of increasing time except when
     * the mmsi changes (it can!).
     * 
     * @return an operator to detect drift candidates
     */
    private static Operator<DriftCandidate, HasFix> detectDriftCandidates() {
        return new DriftingDetectorOperator();
    }

    public static DriftingTransformer detectDrift() {
        return new DriftingTransformer();
    }

    private static class DriftingTransformer implements Transformer<HasFix, DriftCandidate> {

        private final DriftingDetector d = new DriftingDetector();

        @Override
        public Observable<DriftCandidate> call(Observable<HasFix> o) {
            return d.getCandidates(o);
        }
    }

}
