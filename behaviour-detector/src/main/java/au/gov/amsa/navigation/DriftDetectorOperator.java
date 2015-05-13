package au.gov.amsa.navigation;

import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasFix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.util.RingBuffer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * This operator expects a stream of fixes of increasing time except when the
 * mmsi changes (it can!).
 */
public class DriftDetectorOperator implements Operator<DriftCandidate, HasFix> {

    // For a drifting vessel we expect the reporting interval to be 10 seconds
    // (mandated by its speed in knots).
    private static final long MIN_INTERVAL_BETWEEN_FIXES = 10;
    private final Options options;
    private final Func1<Fix, Boolean> isCandidate;

    public DriftDetectorOperator(Options options) {
        this.options = options;
        isCandidate = isCandidate(options);
    }

    public DriftDetectorOperator() {
        this(Options.instance());
    }

    @Override
    public Subscriber<? super HasFix> call(final Subscriber<? super DriftCandidate> child) {
        return new Subscriber<HasFix>(child) {
            final int size = (int) (options.windowSizeMs() * 3 / MIN_INTERVAL_BETWEEN_FIXES / 2);
            final AtomicLong driftingSinceTime = new AtomicLong(Long.MAX_VALUE);
            final AtomicLong nonDriftingSinceTime = new AtomicLong(Long.MAX_VALUE);
            final AtomicLong currentMmsi = new AtomicLong(-1);
            final RingBuffer<FixAndStatus> q = RingBuffer.create(size);

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
                handleFix(f.fix(), q, child, driftingSinceTime, nonDriftingSinceTime, currentMmsi);
            }

        };
    }

    private static class FixAndStatus {
        final Fix fix;
        final boolean drifting;

        public FixAndStatus(Fix fix, boolean drifting) {
            this.fix = fix;
            this.drifting = drifting;
        }

    }

    private void handleFix(Fix f, RingBuffer<FixAndStatus> q,
            Subscriber<? super DriftCandidate> child, AtomicLong driftingSinceTime,
            AtomicLong nonDriftingSinceTime, AtomicLong currentMmsi) {
        // when a fix arrives that is a drift detection start building a queue
        // of fixes. If a certain proportion of fixes are drift detection with a
        // minimum window of report time from the first detection report time
        // then report them to the child subscriber
        if (currentMmsi.get() != f.mmsi() || q.size() == q.maxSize()) {
            // note that hitting maxSize in q should only happen for rubbish
            // mmsi codes like 0 so we are happy to clear the q
            q.clear();
            driftingSinceTime.set(Long.MAX_VALUE);
            nonDriftingSinceTime.set(Long.MAX_VALUE);
            currentMmsi.set(f.mmsi());
        }
        if (q.isEmpty()) {
            if (isCandidate.call(f)) {
                q.push(new FixAndStatus(f, true));
                // reset non drifting time because drifting detected
                nonDriftingSinceTime.set(Long.MAX_VALUE);
                // if drifting since not set then use this fix time
                driftingSinceTime.compareAndSet(Long.MAX_VALUE, f.time());
            } else {
                // if non drifting start time not set then use this fix
                nonDriftingSinceTime.compareAndSet(Long.MAX_VALUE, f.time());
                return;
            }
        } else {
            if (q.peek().fix.time() < f.time()) {
                // queue is non-empty so add to the queue
                q.push(new FixAndStatus(f, isCandidate.call(f)));
            }
        }

        // process the queue if time interval long enough
        if (f.time() - q.peek().fix.time() >= options.windowSizeMs()) {
            // count the number of candidates
            int count = countDrifting(q);
            // if a decent number of drift candidates found in the time interval
            // then emit them
            if ((double) count / q.size() >= options.minProportion()) {
                emitDriftersAndUpdateTimes(q, child, driftingSinceTime, nonDriftingSinceTime,
                        options);
            }
        }

    }

    private static void emitDriftersAndUpdateTimes(RingBuffer<FixAndStatus> q,
            Subscriber<? super DriftCandidate> child, AtomicLong driftingSinceTime,
            AtomicLong nonDriftingSinceTime, Options options) {
        Enumeration<FixAndStatus> en = q.values();
        while (en.hasMoreElements()) {
            FixAndStatus x = en.nextElement();
            q.pop();
            if (x.drifting) {
                // emit DriftCandidate with driftingSinceTime
                long driftingSince;
                if (x.fix.time() - nonDriftingSinceTime.get() > options.nonDriftingThresholdMs())
                    driftingSince = x.fix.time();
                else
                    driftingSince = driftingSinceTime.get();
                child.onNext(new DriftCandidate(x.fix, driftingSince));
                nonDriftingSinceTime.set(Long.MAX_VALUE);
            } else {
                nonDriftingSinceTime.compareAndSet(Long.MAX_VALUE, x.fix.time());
            }
        }
    }

    private static int countDrifting(RingBuffer<FixAndStatus> q) {
        int count = 0;
        Enumeration<FixAndStatus> en = q.values();
        while (en.hasMoreElements()) {
            if (en.nextElement().drifting)
                count++;
        }
        return count;
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
        private static final long DEFAULT_WINDOW_SIZE_MS = TimeUnit.MINUTES.toMillis(5);
        private static final double DEFAULT_MIN_PROPORTION = 0.5;
        private static final long DEFAULT_NON_DRIFTING_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(5);

        private final int minHeadingCogDifference;
        private final int maxHeadingCogDifference;
        private final float minDriftingSpeedKnots;
        private final float maxDriftingSpeedKnots;
        private final long windowSizeMs;
        private final double minProportion;
        private final long nonDriftingThresholdMs;

        private static class Holder {

            static Options INSTANCE = new Options(DEFAULT_HEADING_COG_DIFFERENCE_MIN,
                    DEFAULT_HEADING_COG_DIFFERENCE_MAX, DEFAULT_MIN_DRIFTING_SPEED_KNOTS,
                    DEFAULT_MAX_DRIFTING_SPEED_KNOTS, DEFAULT_WINDOW_SIZE_MS,
                    DEFAULT_MIN_PROPORTION, DEFAULT_NON_DRIFTING_THRESHOLD_MS);
        }

        public static Options instance() {
            return Holder.INSTANCE;
        }

        public Options(int minHeadingCogDifference, int maxHeadingCogDifference,
                float minDriftingSpeedKnots, float maxDriftingSpeedKnots, long windowSizeMs,
                double minProportion, long nonDriftingThresholdMs) {
            Preconditions.checkArgument(minHeadingCogDifference >= 0);
            Preconditions.checkArgument(minDriftingSpeedKnots >= 0);
            Preconditions.checkArgument(minHeadingCogDifference <= maxHeadingCogDifference);
            Preconditions.checkArgument(minDriftingSpeedKnots <= maxDriftingSpeedKnots);
            this.minHeadingCogDifference = minHeadingCogDifference;
            this.maxHeadingCogDifference = maxHeadingCogDifference;
            this.minDriftingSpeedKnots = minDriftingSpeedKnots;
            this.maxDriftingSpeedKnots = maxDriftingSpeedKnots;
            this.windowSizeMs = windowSizeMs;
            this.minProportion = minProportion;
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

        public long windowSizeMs() {
            return windowSizeMs;
        }

        public double minProportion() {
            return minProportion;
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
            b.append(", windowSizeMs=");
            b.append(windowSizeMs);
            b.append(", minProportion=");
            b.append(minProportion);
            b.append(", nonDriftingThresholdMs=");
            b.append(nonDriftingThresholdMs);
            b.append("]");
            return b.toString();
        }

    }

}
