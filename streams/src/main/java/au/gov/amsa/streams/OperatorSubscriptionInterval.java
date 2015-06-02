package au.gov.amsa.streams;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.observers.Subscribers;

import com.github.davidmoten.rx.subjects.PublishSubjectSingleSubscriber;

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
            final PublishSubjectSingleSubscriber<T> subject = PublishSubjectSingleSubscriber
                    .create();
            Worker worker = scheduler.createWorker();
            worker.schedule(() -> {
                subject.unsafeSubscribe(child);
            }, duration, units);
            child.add(worker);
            parent = Subscribers.from(subject);
        } else {
            parent = Subscribers.from(child);
        }
        child.add(parent);
        return parent;
    }
}
