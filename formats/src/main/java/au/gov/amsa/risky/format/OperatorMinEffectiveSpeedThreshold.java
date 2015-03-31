package au.gov.amsa.risky.format;

import java.util.Enumeration;
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
    private final long smallestReportingIntervalMs = 3000;
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
                // if mmsi changes then clear the fix history
                if (!buffer.isEmpty() && buffer.peek().fix().mmsi() != fix.fix().mmsi()) {
                    buffer.clear();
                    middle = Optional.absent();
                }
                buffer.push(fix);
                HasFix latest = fix;
                HasFix first = buffer.peek();
                if (!middle.isPresent()) {
                    if (latest.fix().time() - first.fix().time() > deltaMs) {
                        middle = Optional.of(latest);
                    }
                } else if (latest.fix().time() - middle.get().fix().time() > deltaMs) {
                    // now can emit middle with its pre and post effective speed
                    // and reliability measure (time difference minus deltaMs)

                    // measure distance from first to middle
                    double distanceFirstToMiddleKm = 0;
                    Optional<HasFix> last = Optional.absent();
                    Enumeration<HasFix> en = buffer.values();
                    boolean keepGoing = en.hasMoreElements();
                    while (keepGoing) {
                        HasFix f = en.nextElement();
                        if (last.isPresent())
                            distanceFirstToMiddleKm += distanceKm(last.get(), f);
                        keepGoing = en.hasMoreElements() && f != middle;
                    }

                    // measure distance from middle to latest
                    double distanceMiddleToLatestKm = 0;
                    last = middle;
                    keepGoing = en.hasMoreElements();
                    Optional<HasFix> firstAfterMiddle = Optional.absent();
                    while (keepGoing) {
                        HasFix f = en.nextElement();
                        if (!firstAfterMiddle.isPresent())
                            firstAfterMiddle = Optional.of(f);
                        if (last.isPresent())
                            distanceFirstToMiddleKm += distanceKm(last.get(), f);
                        keepGoing = en.hasMoreElements();
                    }

                    // time from first to middle
                    long timeFirstToMiddleMs = middle.get().fix().time() - first.fix().time();
                    long timeMiddleToLatestMs = latest.fix().time() - middle.get().fix().time();

                    // speed calcs
                    double preSpeedKnots = distanceFirstToMiddleKm / (double) timeFirstToMiddleMs
                            / 1.852 * TimeUnit.HOURS.toMillis(1);

                    double postSpeedKnots = distanceMiddleToLatestKm
                            / (double) timeMiddleToLatestMs / 1.852 * TimeUnit.HOURS.toMillis(1);

                    double preError = Math.abs(middle.get().fix().time() - first.fix().time()
                            - deltaMs)
                            / (double) TimeUnit.HOURS.toMillis(1);
                    double postError = Math.abs(latest.fix().time() - middle.get().fix().time()
                            - deltaMs)
                            / (double) TimeUnit.HOURS.toMillis(1);

                    // emit what we have!
                    child.onNext(new FixWithPreAndPostEffectiveSpeed(fix, preSpeedKnots, preError,
                            postSpeedKnots, postError));

                    // drop values from front of buffer
                    en = buffer.values();
                    // skip first
                    en.nextElement();
                    while (en.hasMoreElements()) {
                        HasFix next = en.nextElement();
                        if (firstAfterMiddle.get().fix().time() - next.fix().time() < deltaMs)
                            break;
                        else
                            buffer.pop();
                    }
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
        private final Fix fix;

        public FixWithPreAndPostEffectiveSpeed(HasFix fix, double preEffectiveSpeedKnots,
                double preError, double postEffectiveSpeedKnots, double postError) {
            this.preEffectiveSpeedKnots = preEffectiveSpeedKnots;
            this.preError = preError;
            this.postEffectiveSpeedKnots = postEffectiveSpeedKnots;
            this.postError = postError;
            this.fix = fix.fix();
        }

        @Override
        public Fix fix() {
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

    }
}
