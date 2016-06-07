package au.gov.amsa.navigation;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.rx.StateMachine.Transition;
import com.github.davidmoten.rx.Transformers;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;
import au.gov.amsa.risky.format.NavigationalStatus;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func1;

public class DriftDetector {

    public static Observable<DriftCandidate> getCandidates(Observable<HasFix> o, Options options) {
        return o.compose(new DriftDetectorTransformer(options));
    }

    public static DriftDetectorTransformer detectDrift() {
        return new DriftDetectorTransformer(Options.instance());
    }

    public static DriftDetectorTransformer detectDrift(Options options) {
        return new DriftDetectorTransformer(options);
    }

    public static class DriftDetectorTransformer implements Transformer<HasFix, DriftCandidate> {

        // because many of these are expected to be in existence simultaneously
        // (one per vessel and between 30,000 and 40,000 vessels may appear in
        // our coverage annually, we need to be nice and careful with how much
        // memory this operator uses.

        private static final long NOT_DRIFTING = Long.MAX_VALUE;
        private static final long MMSI_NOT_SET = 0;

        private final Options options;
        private final Func1<Fix, Boolean> isCandidate;

        public DriftDetectorTransformer(Options options) {
            this.options = options;
            this.isCandidate = isCandidate(options);
        }

        @Override
        public Observable<DriftCandidate> call(Observable<HasFix> o) {
            Transformer<HasFix, DriftCandidate> t = Transformers.stateMachine()
                    .initialState(new State(options)) //
                    .transition(new Transition<State, HasFix, DriftCandidate>() {

                        @Override
                        public State call(State state, HasFix value,
                                Subscriber<DriftCandidate> subscriber) {
                            state.onNext(value, subscriber, isCandidate);
                            return state;
                        }
                    }) //
                    .build();
            return o.compose(t);
        }

        // mutable class but is mutated serially (google Observable contract)
        private static final class State {
            Item a;
            Item b;
            long driftingSince = NOT_DRIFTING;
            long mmsi = 0;
            private final Options options;

            public State(Options options) {
                this.options = options;
            }

            public void onNext(HasFix f, Subscriber<DriftCandidate> subscriber,
                    Func1<Fix, Boolean> isCandidate) {
                try {
                    // Note that it is assumed that the input stream is grouped
                    // by
                    // mmsi and sorted by ascending time.
                    Fix fix = f.fix();

                    if (mmsi != MMSI_NOT_SET && fix.mmsi() != mmsi) {
                        // reset for a new vessel
                        a = null;
                        b = null;
                        driftingSince = NOT_DRIFTING;
                    }
                    mmsi = fix.mmsi();

                    if (outOfTimeOrder(fix)) {
                        return;
                    }

                    final Item item;
                    if (isCandidate.call(fix)) {
                        item = new Drifter(f, false);
                    } else
                        item = new NonDrifter(fix.time());
                    if (a == null) {
                        a = item;
                        processAB(subscriber);
                    } else if (b == null) {
                        b = item;
                        processAB(subscriber);
                    } else {
                        processABC(item, subscriber);
                    }
                } catch (RuntimeException e) {
                    subscriber.onError(e);
                }
            }

            private boolean outOfTimeOrder(Fix fix) {
                if (b != null && fix.time() < b.time())
                    return true;
                else if (a != null && fix.time() < a.time())
                    return true;
                else
                    return false;
            }

            private void processABC(Item c, Subscriber<DriftCandidate> subscriber) {
                if (isDrifter(a) && !isDrifter(b) && !isDrifter(c)) {
                    // ignore c
                    // rule 4, 5
                } else if (isDrifter(a) && !isDrifter(b) && isDrifter(c)) {
                    // rule 6, 7
                    if (withinNonDriftingThreshold(b, c)) {
                        b = c;
                        processAB(subscriber);
                    } else {
                        a = c;
                        b = null;
                    }
                } else {
                    System.out.println(a + "," + b + "," + c);
                    unexpected();
                }
            }

            private void unexpected() {
                throw new RuntimeException("unexpected");
            }

            private void processAB(Subscriber<DriftCandidate> subscriber) {
                if (!isDrifter(a)) {
                    // rule 1
                    a = null;
                    if (b != null)
                        unexpected();
                } else if (b == null) {
                    // do nothing
                } else if (!a.emitted()) {
                    if (isDrifter(b)) {
                        // rule 2
                        if (!expired(a, b)) {
                            driftingSince = a.time();
                            subscriber.onNext(new DriftCandidate(a.fix(), a.time()));
                            subscriber.onNext(new DriftCandidate(b.fix(), a.time()));
                            // mark as emitted
                            a = new Drifter(a.fix(), true);
                            b = null;
                        } else {
                            a = b;
                            b = null;
                        }
                    }
                } else {
                    // a has been emitted
                    // rule 3
                    if (isDrifter(b)) {
                        if (!expired(a, b)) {
                            subscriber.onNext(new DriftCandidate(b.fix(), driftingSince));
                            a = new Drifter(b.fix(), true);
                            b = null;
                        } else {
                            a = b;
                            b = null;
                        }
                    }
                }
            }

            private boolean expired(Item a, Item b) {
                return b.time() - a.time() >= options.expiryAgeMs();
            }

            private boolean withinNonDriftingThreshold(Item a, Item b) {
                return b.time() - a.time() < options.nonDriftingThresholdMs();
            }

        }

        private static boolean isDrifter(Item item) {
            return item instanceof Drifter;
        }

        private static interface Item {
            long time();

            HasFix fix();

            boolean emitted();
        }

        private static class Drifter implements Item {

            private final HasFix fix;
            private final boolean emitted;

            Drifter(HasFix fix, boolean emitted) {
                this.fix = fix;
                this.emitted = emitted;
            }

            @Override
            public long time() {
                return fix.fix().time();
            }

            @Override
            public HasFix fix() {
                return fix;
            }

            @Override
            public boolean emitted() {
                return emitted;
            }

        }

        private static class NonDrifter implements Item {

            private final long time;

            NonDrifter(long time) {
                this.time = time;
            }

            @Override
            public long time() {
                return time;
            }

            @Override
            public Fix fix() {
                throw new RuntimeException("unexpected");
            }

            @Override
            public boolean emitted() {
                // never gets emitted
                return false;
            }

        }

    }

    @VisibleForTesting
    static Func1<Fix, Boolean> isCandidate(Options options) {
        return f -> {
            if (f.courseOverGroundDegrees().isPresent() && f.headingDegrees().isPresent()
                    && f.speedOverGroundKnots().isPresent()
                    && (!f.navigationalStatus().isPresent()
                            || (f.navigationalStatus().get() != NavigationalStatus.AT_ANCHOR && f
                                    .navigationalStatus().get() != NavigationalStatus.MOORED))) {
                double diff = diff(f.courseOverGroundDegrees().get(), f.headingDegrees().get());
                return diff >= options.minHeadingCogDifference()
                        && diff <= options.maxHeadingCogDifference()
                        && f.speedOverGroundKnots().get() <= options.maxDriftingSpeedKnots()
                        && f.speedOverGroundKnots().get() > options.minDriftingSpeedKnots();
            } else
                return false;
        };
    }

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

    public static final class Options {

        @VisibleForTesting
        static final int DEFAULT_HEADING_COG_DIFFERENCE_MIN = 45;
        @VisibleForTesting
        static final int DEFAULT_HEADING_COG_DIFFERENCE_MAX = 135;
        @VisibleForTesting
        static final float DEFAULT_MIN_DRIFTING_SPEED_KNOTS = 0.25f;
        @VisibleForTesting
        static final float DEFAULT_MAX_DRIFTING_SPEED_KNOTS = 20;
        private static final long DEFAULT_EXPIRY_AGE_MS = TimeUnit.HOURS.toMillis(6);
        private static final long DEFAULT_NON_DRIFTING_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(5);

        private final int minHeadingCogDifference;
        private final int maxHeadingCogDifference;
        private final float minDriftingSpeedKnots;
        private final float maxDriftingSpeedKnots;
        private final long expiryAgeMs;
        private final long nonDriftingThresholdMs;

        private static class Holder {

            static Options INSTANCE = new Options(DEFAULT_HEADING_COG_DIFFERENCE_MIN,
                    DEFAULT_HEADING_COG_DIFFERENCE_MAX, DEFAULT_MIN_DRIFTING_SPEED_KNOTS,
                    DEFAULT_MAX_DRIFTING_SPEED_KNOTS, DEFAULT_EXPIRY_AGE_MS,
                    DEFAULT_NON_DRIFTING_THRESHOLD_MS);
        }

        public static Options instance() {
            return Holder.INSTANCE;
        }

        public Options(int minHeadingCogDifference, int maxHeadingCogDifference,
                float minDriftingSpeedKnots, float maxDriftingSpeedKnots, long expiryAgeMs,
                long nonDriftingThresholdMs) {
            Preconditions.checkArgument(minHeadingCogDifference >= 0);
            Preconditions.checkArgument(minDriftingSpeedKnots >= 0);
            Preconditions.checkArgument(minHeadingCogDifference <= maxHeadingCogDifference);
            Preconditions.checkArgument(minDriftingSpeedKnots <= maxDriftingSpeedKnots);
            Preconditions.checkArgument(expiryAgeMs > 0);
            Preconditions.checkArgument(nonDriftingThresholdMs >= 0);
            this.minHeadingCogDifference = minHeadingCogDifference;
            this.maxHeadingCogDifference = maxHeadingCogDifference;
            this.minDriftingSpeedKnots = minDriftingSpeedKnots;
            this.maxDriftingSpeedKnots = maxDriftingSpeedKnots;
            this.expiryAgeMs = expiryAgeMs;
            this.nonDriftingThresholdMs = nonDriftingThresholdMs;
        }

        public int maxHeadingCogDifference() {
            return maxHeadingCogDifference;
        }

        public int minHeadingCogDifference() {
            return minHeadingCogDifference;
        }

        public float maxDriftingSpeedKnots() {
            return maxDriftingSpeedKnots;
        }

        public float minDriftingSpeedKnots() {
            return minDriftingSpeedKnots;
        }

        public long expiryAgeMs() {
            return expiryAgeMs;
        }

        public long nonDriftingThresholdMs() {
            return nonDriftingThresholdMs;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Options [minHeadingCogDifference=");
            b.append(minHeadingCogDifference);
            b.append(", maxHeadingCogDifference=");
            b.append(maxHeadingCogDifference);
            b.append(", minDriftingSpeedKnots=");
            b.append(minDriftingSpeedKnots);
            b.append(", maxDriftingSpeedKnots=");
            b.append(maxDriftingSpeedKnots);
            b.append(", expiryAgeMs=");
            b.append(expiryAgeMs);
            b.append(", nonDriftingThresholdMs=");
            b.append(nonDriftingThresholdMs);
            b.append("]");
            return b.toString();
        }

    }

}
