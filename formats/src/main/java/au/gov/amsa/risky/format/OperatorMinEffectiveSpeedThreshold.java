package au.gov.amsa.risky.format;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.risky.format.OperatorMinEffectiveSpeedThreshold.FixWithPreAndPostEffectiveSpeed;
import au.gov.amsa.util.RingBuffer;

import com.github.davidmoten.grumpy.core.Position;
import com.google.common.base.Optional;

public final class OperatorMinEffectiveSpeedThreshold implements
        Operator<FixWithPreAndPostEffectiveSpeed, HasFix> {

    private long deltaMs;
    private final long smallestReportingIntervalMs = 1000;
    private final RingBuffer<HasFix> buffer;

    public OperatorMinEffectiveSpeedThreshold(long deltaMs) {
        this.deltaMs = deltaMs;
        int maxSize = (int) (deltaMs / smallestReportingIntervalMs) + 1;
        this.buffer = RingBuffer.create(maxSize);
    }

    @Override
    public Subscriber<? super HasFix> call(
            final Subscriber<? super FixWithPreAndPostEffectiveSpeed> child) {
        return new Subscriber<HasFix>(child) {

            private Optional<HasFix> middle = Optional.absent();

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(HasFix fix) {
                boolean emitted = false;
                // if mmsi changes then clear the fix history
                if (!buffer.isEmpty() && buffer.peek().fix().mmsi() != fix.fix().mmsi()) {
                    buffer.clear();
                    middle = Optional.absent();
                }
                buffer.add(fix);
                HasFix latest = fix;
                if (!middle.isPresent()) {
                    HasFix first = buffer.peek();
                    if (latest.fix().time() - first.fix().time() >= deltaMs) {
                        middle = Optional.of(latest);
                    }
                } else
                    while (latest.fix().time() - middle.get().fix().time() >= deltaMs) {

                        // now can emit middle with its pre and post effective
                        // speed and reliability measure (time difference minus
                        // deltaMs)

                        HasFix first = buffer.peek();

                        // measure distance from first to middle
                        double distanceFirstToMiddleKm = 0;
                        Iterator<HasFix> en = buffer.iterator();
                        {
                            Optional<HasFix> previous = Optional.absent();
                            boolean keepGoing = en.hasNext();
                            while (keepGoing) {
                                HasFix f = en.next();
                                if (previous.isPresent())
                                    distanceFirstToMiddleKm += distanceKm(previous.get(), f);
                                previous = Optional.of(f);
                                keepGoing = en.hasNext() && f != middle.get();
                            }
                        }

                        // measure distance from middle to latest
                        double distanceMiddleToLatestKm = 0;
                        Optional<HasFix> firstAfterMiddle = Optional.absent();
                        {
                            Optional<HasFix> previous = middle;
                            boolean keepGoing = en.hasNext();
                            while (keepGoing) {
                                HasFix f = en.next();
                                if (!firstAfterMiddle.isPresent())
                                    firstAfterMiddle = Optional.of(f);
                                distanceMiddleToLatestKm += distanceKm(previous.get(), f);
                                previous = Optional.of(f);
                                keepGoing = en.hasNext();
                            }
                        }

                        // time from first to middle
                        long timeFirstToMiddleMs = middle.get().fix().time() - first.fix().time();
                        long timeMiddleToLatestMs = latest.fix().time() - middle.get().fix().time();

                        // speed calcs
                        double preSpeedKnots = distanceFirstToMiddleKm
                                / (double) timeFirstToMiddleMs / 1.852 * TimeUnit.HOURS.toMillis(1);

                        double postSpeedKnots = distanceMiddleToLatestKm
                                / (double) timeMiddleToLatestMs / 1.852
                                * TimeUnit.HOURS.toMillis(1);

                        double preError = Math.abs(middle.get().fix().time() - first.fix().time()
                                - deltaMs)
                                / (double) TimeUnit.MINUTES.toMillis(1);
                        double postError = Math.abs(latest.fix().time() - middle.get().fix().time()
                                - deltaMs)
                                / (double) TimeUnit.MINUTES.toMillis(1);

                        // emit what we have!
                        child.onNext(new FixWithPreAndPostEffectiveSpeed(middle.get(),
                                preSpeedKnots, preError, postSpeedKnots, postError));
                        emitted = true;

                        // drop values from front of buffer
                        en = buffer.iterator();
                        // skip first
                        en.next();
                        while (en.hasNext()) {
                            HasFix next = en.next();
                            if (firstAfterMiddle.get().fix().time() - next.fix().time() < deltaMs)
                                break;
                            else
                                buffer.remove();
                        }
                        middle = firstAfterMiddle;
                    }
                if (!emitted) {
                    // value submitted to this method did not result in an
                    // emission so request another one
                    request(1);
                }
            }

        };
    }

    private static double distanceKm(HasFix a, HasFix b) {
        return toPosition(a).getDistanceToKm(toPosition(b));
    }

    private static Position toPosition(HasFix f) {
        return Position.create(f.fix().lat(), f.fix().lon());
    }

    public static final class FixWithPreAndPostEffectiveSpeed implements HasFix {

        private final double preEffectiveSpeedKnots;

        private final double preError;
        private final double postEffectiveSpeedKnots;
        private final double postError;
        private final HasFix fix;

        public FixWithPreAndPostEffectiveSpeed(HasFix fix, double preEffectiveSpeedKnots,
                double preError, double postEffectiveSpeedKnots, double postError) {
            this.preEffectiveSpeedKnots = preEffectiveSpeedKnots;
            this.preError = preError;
            this.postEffectiveSpeedKnots = postEffectiveSpeedKnots;
            this.postError = postError;
            this.fix = fix;
        }

        @Override
        public Fix fix() {
            return fix.fix();
        }

        public HasFix fixWrapper() {
            return fix;
        }

        public double preEffectiveSpeedKnots() {
            return preEffectiveSpeedKnots;
        }

        public double preError() {
            return preError;
        }

        public double postEffectiveSpeedKnots() {
            return postEffectiveSpeedKnots;
        }

        public double postError() {
            return postError;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("FixWithPreAndPostEffectiveSpeed [preEffectiveSpeedKnots=");
            b.append(preEffectiveSpeedKnots);
            b.append(", preError=");
            b.append(preError);
            b.append(", postEffectiveSpeedKnots=");
            b.append(postEffectiveSpeedKnots);
            b.append(", postError=");
            b.append(postError);
            b.append(", fix=");
            b.append(fix);
            b.append("]");
            return b.toString();
        }

    }
}
