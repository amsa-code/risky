package au.gov.amsa.navigation;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import au.gov.amsa.navigation.DriftDetectorOperator.Options;
import au.gov.amsa.risky.format.HasFix;

public class DriftDetector {

    public Observable<DriftCandidate> getCandidates(Observable<HasFix> o, Options options) {
        return o.lift(detectDriftCandidates(options));
    }

    public static DriftDetectorTransformer detectDrift() {
        return new DriftDetectorTransformer(Options.instance());
    }

    /**
     * This operator expects a stream of fixes of increasing time except when
     * the mmsi changes (it can!).
     * 
     * @return an operator to detect drift candidates
     */
    private static Operator<DriftCandidate, HasFix> detectDriftCandidates(Options options) {
        return new DriftDetectorOperator(options);
    }

    public static DriftDetectorTransformer detectDrift(Options options) {
        return new DriftDetectorTransformer(options);
    }

    public static class DriftDetectorTransformer implements Transformer<HasFix, DriftCandidate> {

        private final DriftDetector d = new DriftDetector();
        private final Options options;

        public DriftDetectorTransformer(Options options) {
            this.options = options;
        }

        @Override
        public Observable<DriftCandidate> call(Observable<HasFix> o) {
            return d.getCandidates(o, options).onBackpressureBuffer();
        }
    }

}
