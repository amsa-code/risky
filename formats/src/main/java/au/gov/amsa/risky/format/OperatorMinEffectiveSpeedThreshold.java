package au.gov.amsa.risky.format;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.util.RingBuffer;

public class OperatorMinEffectiveSpeedThreshold implements Operator<HasFix, HasFix> {

    private long deltaMs;
    private final long smallestReportingIntervalMs = 3000;
    private final RingBuffer<Fix> buffer;

    public OperatorMinEffectiveSpeedThreshold(long deltaMs) {
        this.deltaMs = deltaMs;
        int maxSize = (int) (deltaMs / smallestReportingIntervalMs) + 1;
        this.buffer = RingBuffer.create(maxSize);
    }

    @Override
    public Subscriber<? super HasFix> call(Subscriber<? super HasFix> t1) {
        // TODO Auto-generated method stub
        return null;
    }
}
