package au.gov.amsa.streams;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.observers.Subscribers;
import rx.subjects.PublishSubject;

public class OperatorSubscriptionInterval<T> implements Operator<T, T> {

    private final long duration;
    private final TimeUnit units;
    private final Scheduler scheduler;

    public OperatorSubscriptionInterval(long duration, TimeUnit units, Scheduler scheduler) {
        this.duration = duration;
        this.units = units;
        this.scheduler = scheduler;
    }

    private final AtomicBoolean firstTime = new AtomicBoolean(true);

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        Subscriber<T> parent;
        if (firstTime.compareAndSet(true, false)) {
            // don't delay subscription for the first time
            parent = Subscribers.from(child);
        } else {
            final PublishSubject<T> subject = PublishSubject.create();
            Worker worker = scheduler.createWorker();
            worker.schedule(() -> {
                subject.unsafeSubscribe(child);
            }, duration, units);
            child.add(worker);
            parent = Subscribers.from(subject);
        }
        child.add(parent);
        return parent;
    }
}
