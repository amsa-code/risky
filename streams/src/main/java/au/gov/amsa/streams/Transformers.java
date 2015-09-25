package au.gov.amsa.streams;

import java.util.concurrent.TimeUnit;

import rx.Observable.Transformer;
import rx.Scheduler;

public class Transformers {

    public static <T> Transformer<T, T> subscriptionInterval(long delay, TimeUnit unit,
            Scheduler scheduler) {
        return o -> o.lift(new OperatorSubscriptionInterval<T>(delay, unit, scheduler));
    }
}
