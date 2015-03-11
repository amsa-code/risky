package au.gov.amsa.navigation;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.util.RingBuffer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class DriftingDetectorFix {

    private static final Logger log = LoggerFactory.getLogger(DriftingDetectorFix.class);

    static final double KNOTS_TO_METRES_PER_SECOND = 0.5144444;
    @VisibleForTesting
    static final int HEADING_COG_DIFFERENCE_MIN = 70;
    @VisibleForTesting
    static final int HEADING_COG_DIFFERENCE_MAX = 110;
    @VisibleForTesting
    static final double MAX_DRIFTING_SPEED_KNOTS = 4;
    @VisibleForTesting
    static final double MIN_DRIFTING_SPEED_KNOTS = 0.3;

    private static final long WINDOW_SIZE_MS = 300 * 1000;

    private static final double MIN_PROPORTION = 0.5;

    public Observable<Fix> getCandidates(Observable<Fix> o) {
        return o.lift(new Operator<Fix, Fix>() {

            @Override
            public Subscriber<? super Fix> call(final Subscriber<? super Fix> child) {
                return new Subscriber<Fix>(child) {
                    RingBuffer<Fix> q = RingBuffer.create(1000);

                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Fix f) {
                        log.info("fix=" + f);
                        if (q.isEmpty()) {
                            if (IS_CANDIDATE.call(f))
                                q.push(f);
                        } else if (q.peek().getMmsi() != f.getMmsi()) {
                            q.clear().push(f);
                        } else {
                            try {
                                q.push(f);
                            } catch (RuntimeException e) {
                                log.error("fix=" + f);
                                throw e;
                            }
                            if (f.getTime() - q.peek().getTime() > WINDOW_SIZE_MS) {
                                int count = 0;
                                Enumeration<Fix> en = q.values();
                                while (en.hasMoreElements()) {
                                    if (IS_CANDIDATE.call(en.nextElement()))
                                        count++;
                                }
                                if ((double) count / q.size() >= MIN_PROPORTION) {
                                    en = q.values();
                                    while (en.hasMoreElements()) {
                                        if (q.isEmpty())
                                            log.info("empty!");
                                        Fix x = en.nextElement();
                                        q.pop();
                                        if (IS_CANDIDATE.call(x))
                                            child.onNext(x);
                                    }
                                }
                            }
                        }
                    }

                };
            }
        });
    }

    public static DriftingTransformer detectDrift() {
        return new DriftingTransformer();
    }

    private static class DriftingTransformer implements Transformer<Fix, Fix> {

        private final DriftingDetectorFix d = new DriftingDetectorFix();

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
