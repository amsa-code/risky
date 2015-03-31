package au.gov.amsa.risky.format;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.risky.format.OperatorMinEffectiveSpeedThreshold.FixWithPreAndPostEffectiveSpeed;
import au.gov.amsa.util.RingBuffer;

import com.google.common.base.Optional;

public class OperatorMinEffectiveSpeedThreshold implements
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

            private Optional<HasFix> latest = Optional.absent();
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
                }
                buffer.push(fix);
                latest = Optional.of(fix);
                HasFix first = buffer.peek();
                if (!middle.isPresent()) {
                    if (latest.get().fix().time() - first.fix().time() > deltaMs) {
                        middle = latest;
                    }
                } else if (latest.get().fix().time() - middle.get().fix().time() > deltaMs) {

                }
            }
        };
    }

    public static class FixWithPreAndPostEffectiveSpeed implements HasFix {

        @Override
        public Fix fix() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
