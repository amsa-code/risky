package au.gov.amsa.navigation;

import java.util.concurrent.TimeUnit;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;
import au.gov.amsa.risky.format.NavigationalStatus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public final class DriftDetectorOperator2 implements Operator<DriftCandidate, HasFix> {

    private final Func1<Fix, Boolean> isCandidate;
    private final Options options;

    public DriftDetectorOperator2(Options options) {
        this.isCandidate = isCandidate(options);
        this.options = options;
    }

    // because many of these are expected to be in existence simultaneously (one
    // per vessel and between 30,000 and 40,000 vessels may appear in our
    // coverage annually, we need to be nice and careful with how much memory
    // this operator uses.

    private static final long NOT_DRIFTING = Long.MAX_VALUE;

    @Override
    public Subscriber<? super HasFix> call(Subscriber<? super DriftCandidate> child) {
        return new Subscriber<HasFix>(child) {

            private Item a;
            private Item b;
            private long driftingSince = NOT_DRIFTING;

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(HasFix f) {
                Fix fix = f.fix();
                final Item item;
                if (isCandidate.call(fix))
                    item = new Drifter(fix, false);
                else
                    item = new NonDrifter(fix.time());
                if (a == null) {
                    a = item;
                    processAB();
                } else if (b == null) {
                    b = item;
                    processAB();
                } else {
                    processABC(item);
                }
            }

            private void processABC(Item item) {

            }

            private void processAB() {
                if (!isDrifter(a))
                    a = null;
                else if (!a.emitted()) {
                    if (isDrifter(b)) {
                        if (!expired(a, b)) {
                            driftingSince = a.time();
                            child.onNext(new DriftCandidate(a.fix(), a.time()));
                            child.onNext(new DriftCandidate(b.fix(), a.time()));
                            a = new Drifter(a.fix(), true);
                            b = null;
                        } else {
                            a = b;
                            b = null;
                        }
                    }
                } else {
                    // a has been emitted
                    if (isDrifter(b)) {
                        if (!expired(a, b)) {
                            child.onNext(new DriftCandidate(b.fix(), driftingSince));
                            a = new Drifter(b.fix(), true);
                            b = null;
                        }
                    }
                }
            }
        };
    }

    private boolean expired(Item a, Item b) {
        return b.time() - a.time() >= options.expiryAgeMs();
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
            return null;
        }

        @Override
        public boolean emitted() {
            // never gets emitted
            return false;
        }

    }

    @VisibleForTesting
    static Func1<Fix, Boolean> isCandidate(Options options) {
        return f -> {
            if (f.courseOverGroundDegrees().isPresent()
                    && f.headingDegrees().isPresent()
                    && f.speedOverGroundKnots().isPresent()
                    && (!f.navigationalStatus().isPresent() || (f.navigationalStatus().get() != NavigationalStatus.AT_ANCHOR && f
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
        private static final long DEFAULT_MIN_WINDOW_SIZE_MS = TimeUnit.MINUTES.toMillis(5);
        private static final long DEFAULT_EXPIRY_AGE_MS = TimeUnit.HOURS.toMillis(5);
        private static final double DEFAULT_MIN_PROPORTION = 0.5;
        private static final long DEFAULT_NON_DRIFTING_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(5);
        private static final boolean FAVOUR_MEMORY_OVER_SPEED = true;

        private final int minHeadingCogDifference;
        private final int maxHeadingCogDifference;
        private final float minDriftingSpeedKnots;
        private final float maxDriftingSpeedKnots;
        private final long windowSizeMs;
        private final long expiryAgeMs;
        private final double minProportion;
        private final long nonDriftingThresholdMs;
        private boolean favourMemoryOverSpeed;

        private static class Holder {

            static Options INSTANCE = new Options(DEFAULT_HEADING_COG_DIFFERENCE_MIN,
                    DEFAULT_HEADING_COG_DIFFERENCE_MAX, DEFAULT_MIN_DRIFTING_SPEED_KNOTS,
                    DEFAULT_MAX_DRIFTING_SPEED_KNOTS, DEFAULT_MIN_WINDOW_SIZE_MS,
                    DEFAULT_EXPIRY_AGE_MS, DEFAULT_MIN_PROPORTION,
                    DEFAULT_NON_DRIFTING_THRESHOLD_MS, FAVOUR_MEMORY_OVER_SPEED);
        }

        public static Options instance() {
            return Holder.INSTANCE;
        }

        public Options(int minHeadingCogDifference, int maxHeadingCogDifference,
                float minDriftingSpeedKnots, float maxDriftingSpeedKnots, long windowSizeMs,
                long expiryAgeMs, double minProportion, long nonDriftingThresholdMs,
                boolean favourMemoryOverSpeed) {
            Preconditions.checkArgument(minHeadingCogDifference >= 0);
            Preconditions.checkArgument(minDriftingSpeedKnots >= 0);
            Preconditions.checkArgument(minHeadingCogDifference <= maxHeadingCogDifference);
            Preconditions.checkArgument(minDriftingSpeedKnots <= maxDriftingSpeedKnots);
            Preconditions.checkArgument(expiryAgeMs == 0 || expiryAgeMs > windowSizeMs);
            Preconditions.checkArgument(minProportion >= 0 && minProportion <= 1.0);
            Preconditions.checkArgument(windowSizeMs > 0);
            Preconditions.checkArgument(nonDriftingThresholdMs >= 0);
            this.minHeadingCogDifference = minHeadingCogDifference;
            this.maxHeadingCogDifference = maxHeadingCogDifference;
            this.minDriftingSpeedKnots = minDriftingSpeedKnots;
            this.maxDriftingSpeedKnots = maxDriftingSpeedKnots;
            this.windowSizeMs = windowSizeMs;
            this.expiryAgeMs = expiryAgeMs;
            this.minProportion = minProportion;
            this.nonDriftingThresholdMs = nonDriftingThresholdMs;
            this.favourMemoryOverSpeed = favourMemoryOverSpeed;
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

        public long windowSizeMs() {
            return windowSizeMs;
        }

        public long expiryAgeMs() {
            return expiryAgeMs;
        }

        public double minProportion() {
            return minProportion;
        }

        public long nonDriftingThresholdMs() {
            return nonDriftingThresholdMs;
        }

        public boolean favourMemoryOverSpeed() {
            return favourMemoryOverSpeed;
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
            b.append(", windowSizeMs=");
            b.append(windowSizeMs);
            b.append(", expiryAgeMs=");
            b.append(expiryAgeMs);
            b.append(", minProportion=");
            b.append(minProportion);
            b.append(", nonDriftingThresholdMs=");
            b.append(nonDriftingThresholdMs);
            b.append(", favourMemoryOverSpeed=");
            b.append(favourMemoryOverSpeed);
            b.append("]");
            return b.toString();
        }

    }

}
