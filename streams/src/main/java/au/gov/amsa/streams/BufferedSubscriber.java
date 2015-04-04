package au.gov.amsa.streams;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Subscriber;
import rx.internal.operators.NotificationLite;

public class BufferedSubscriber<T> extends Subscriber<T> {
    // utility object for reactive events
    private final NotificationLite<T> on = NotificationLite.instance();

    private volatile long expected;
    @SuppressWarnings("rawtypes")
    private final AtomicLongFieldUpdater<BufferedSubscriber> EXPECTED = AtomicLongFieldUpdater
            .newUpdater(BufferedSubscriber.class, "expected");

    // queue to hold messages till they are requested
    private final Deque<Object> queue = new LinkedList<Object>();
    private volatile boolean requestAll = false;
    private final Subscriber<? super T> child;

    public BufferedSubscriber(Subscriber<? super T> child) {
        this.child = child;
        this.expected = expected;
    }

    public void requestMore(long n) {
        // this method may be run concurrent with any of the event methods
        // below so watch out for thread safety
        if (requestAll || n <= 0)
            // ignore request if all items have already been requested or if
            // invalid request is received
            return;
        else if (n == Long.MAX_VALUE)
            requestAll = true;
        else
            EXPECTED.addAndGet(this, n);
        request(n);
        drainQueue();
    }

    public boolean requestedAll() {
        return requestAll;
    }

    public void drainQueue() {
        // only used by backpressure path
        while (true) {
            Object item = queue.peek();
            if (item == null || isUnsubscribed())
                break;
            else if (on.isCompleted(item) || on.isError(item)) {
                on.accept(child, queue.poll());
                break;
            } else if (expected == 0)
                break;
            else {
                // expected won't be Long.MAX_VALUE so can safely
                // decrement
                EXPECTED.decrementAndGet(this);
                on.accept(child, queue.poll());
            }
        }
    }

    @Override
    public void onCompleted() {
        queue.add(on.completed());
        drainQueue();
    }

    @Override
    public void onError(Throwable e) {
        if (requestedAll()) {
            // fast path
            child.onError(e);
        } else {
            // backpressure path
            queue.add(on.error(e));
            drainQueue();
        }
    }

    @Override
    public void onNext(T t) {
        queue.add(on.next(t));
        // for optimization purposes the caller should drain the queue
    }

}
