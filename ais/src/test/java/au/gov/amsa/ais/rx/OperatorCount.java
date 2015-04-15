package au.gov.amsa.ais.rx;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

public class OperatorCount<T> implements Operator<Integer, T> {

    @Override
    public Subscriber<? super T> call(Subscriber<? super Integer> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        child.add(parent);
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private Subscriber<? super Integer> child;
        private int count;
        private volatile boolean emit = false;
        private volatile boolean completed = false;
        private final AtomicBoolean emitted = new AtomicBoolean(false);

        ParentSubscriber(Subscriber<? super Integer> child) {
            this.child = child;
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
                child.onNext(count);
                child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            count++;
        }

    }

}
