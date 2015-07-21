package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.EffectiveSpeedChecker.effectiveSpeedOk;
import static com.google.common.base.Optional.of;
import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;

import com.google.common.base.Optional;

/**
 * Given a sequence of fixes the first fix is consider to be the first fix that
 * passes an effective speed check with its following fix. Having established
 * the first fix then following fixes are discarded from the sequence that fail
 * the effective speed check with the last confirmed fix.
 */
public class OperatorEffectiveSpeedFilter implements Operator<Fix, Fix> {

    private final SegmentOptions options;

    public OperatorEffectiveSpeedFilter(SegmentOptions options) {
        this.options = options;
    }

    @Override
    public Subscriber<? super Fix> call(Subscriber<? super Fix> child) {
        Subscriber<Fix> parent = createSubscriber(child, options);
        return parent;

    }

    private static Subscriber<Fix> createSubscriber(final Subscriber<? super Fix> child,
            final SegmentOptions options) {

        return new Subscriber<Fix>(child) {

            private Optional<Fix> previousFix = Optional.absent();
            private Optional<Fix> first = Optional.absent();

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(Fix fix) {

                if (!previousFix.isPresent()) {
                    if (!first.isPresent()) {
                        // buffer the very first fix. It will get emitted only
                        // if passes effective speed check with the following
                        // fix. If it does not then the next fix will be
                        // considered as the next candidate for being the first
                        // fix.
                        first = of(fix);
                        // because no emission we request again to honour
                        // backpressure
                        request(1);
                    } else if (effectiveSpeedOk(first.get(), fix, options)) {
                        previousFix = of(fix);
                        child.onNext(first.get());
                        child.onNext(fix);
                    } else {
                        first = of(fix);
                        // because no emission we request again to honour
                        // backpressure
                        request(1);
                    }
                } else if (effectiveSpeedOk(previousFix.get(), fix, options)) {
                    previousFix = of(fix);
                    child.onNext(fix);
                } else {
                    // because no emission we request again to honour
                    // backpressure
                    request(1);
                    // log.info("eff speed not ok " + previousFix.get() + "->" +
                    // fix);
                }

                // else ignore the fix and try effective speed check with the
                // next one
            }
        };
    }

}
