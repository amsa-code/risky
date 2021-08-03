package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.EffectiveSpeedChecker.effectiveSpeedOk;
import static java.util.Optional.of;

import java.util.Optional;

import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Given a sequence of fixes the first fix is consider to be the first fix that
 * passes an effective speed check with its following fix. Having established
 * the first fix then following fixes are discarded from the sequence that fail
 * the effective speed check with the last confirmed fix.
 */
public class OperatorEffectiveSpeedChecker implements Operator<EffectiveSpeedCheck, Fix> {

    private final SegmentOptions options;

    public OperatorEffectiveSpeedChecker(SegmentOptions options) {
        this.options = options;
    }

    @Override
    public Subscriber<? super Fix> call(Subscriber<? super EffectiveSpeedCheck> child) {
        Subscriber<Fix> parent = createSubscriber(child, options);
        return parent;

    }

    private static Subscriber<Fix> createSubscriber(
            final Subscriber<? super EffectiveSpeedCheck> child, final SegmentOptions options) {

        return new Subscriber<Fix>(child) {

            /**
             * The last emitted fix.
             */
            private Optional<Fix> previousFix = Optional.empty();

            /**
             * The latest fix.
             */
            private Optional<Fix> first = Optional.empty();

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
                        child.onNext(new EffectiveSpeedCheck(first.get(), true));
                        child.onNext(new EffectiveSpeedCheck(fix, true));
                    } else {
                        first = of(fix);
                        // because no emission we request again to honour
                        // backpressure
                        request(1);
                    }
                } else if (effectiveSpeedOk(previousFix.get(), fix, options)) {
                    previousFix = of(fix);
                    child.onNext(new EffectiveSpeedCheck(fix, true));
                } else {
                    // failed effective speed check
                    child.onNext(new EffectiveSpeedCheck(fix, false));
                }
            }
        };
    }

}
