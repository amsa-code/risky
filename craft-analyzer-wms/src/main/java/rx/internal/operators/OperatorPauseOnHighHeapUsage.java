package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorPauseOnHighHeapUsage<T> implements Operator<T, T> {

    private final double heapThresholdPercent;
    private final long pauseMs;
    private final long checkEvery;

    public OperatorPauseOnHighHeapUsage(double heapThresholdPercent, long pauseMs, long checkEvery) {
        this.heapThresholdPercent = heapThresholdPercent;
        this.pauseMs = pauseMs;
        this.checkEvery = checkEvery;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            long count = 0;

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
                count++;
                if (count % checkEvery == 0) {
                    count = 0;
                    Runtime r = Runtime.getRuntime();
                    double heapPercent = 100.0 * (r.totalMemory() - r.freeMemory()) / r.maxMemory();
                    if (heapPercent > heapThresholdPercent)
                        try {
                            System.out.println("pausing for " + pauseMs + "ms");
                            Thread.sleep(pauseMs);
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                }
            }
        };
    }

}
