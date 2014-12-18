package au.gov.amsa.craft.analyzer.wms;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

public class OperatorBackpressureChecker<T> implements Operator<T, T> {

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);

        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.addRequest(n);
                parent.requestMore(n);
            }
        });

        return parent;
    }

    private final static class ParentSubscriber<T> extends Subscriber<T> {
        private final Subscriber<? super T> child;
        private long countEmitted = 0;
        private long countRequests = 0;

        private ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }

        public void addRequest(long n) {
            countRequests += n;
        }

        private void requestMore(long n) {
            request(n);
        }

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
            countEmitted++;
            if (countEmitted > countRequests) {
                child.onError(new RuntimeException("emissions exceeded requested amount!"));
                //eagerly unsubscribe
                unsubscribe();
            }
            else
                child.onNext(t);
        }

    }

}
