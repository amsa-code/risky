package au.gov.amsa.ais.rx;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func2;

public class OperatorReduce<T, R> implements Operator<R, T> {

    private final Func2<R, ? super T, R> reduction;
    private R initialValue;

    public OperatorReduce(Func2<R, ? super T, R> reduction, R initialValue) {
        this.reduction = reduction;
        this.initialValue = initialValue;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> child) {
        final ParentSubscriber<T, R> parent = new ParentSubscriber<T, R>(child, reduction,
                initialValue);
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        child.add(parent);
        return parent;
    }

    private static class ParentSubscriber<T, R> extends Subscriber<T> {

        private final Subscriber<? super R> child;
        private volatile R value;
        private volatile boolean emit = false;
        private volatile boolean completed = false;
        private final AtomicBoolean emitted = new AtomicBoolean(false);
        private final Func2<R, ? super T, R> reduction;

        ParentSubscriber(Subscriber<? super R> child, Func2<R, ? super T, R> reduction,
                R initialValue) {
            this.child = child;
            this.reduction = reduction;
            this.value = initialValue;
        }

        void requestMore(long n) {
            if (n > 0) {
                emit = true;
            }
            // even if request = 0 emit might be true now
            // so we will check again
            drain();
        }

        @Override
        public void onCompleted() {
            completed = true;
            drain();
        }

        void drain() {
            if (completed && emit && emitted.compareAndSet(false, true)) {
                child.onNext(value);
                child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            value = reduction.call(value, t);
        }

    }

}
