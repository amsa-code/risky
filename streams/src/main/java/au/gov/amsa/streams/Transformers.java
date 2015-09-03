package au.gov.amsa.streams;

import java.util.concurrent.TimeUnit;

import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action2;
import rx.functions.Func3;

public class Transformers {

    public static <T> Transformer<T, T> subscriptionInterval(long delay, TimeUnit unit,
            Scheduler scheduler) {
        return o -> o.lift(new OperatorSubscriptionInterval<T>(delay, unit, scheduler));
    }

    private static class LeftOver {
        final String value;

        private LeftOver(String value) {
            this.value = value;
        }
    }

    public static <T> Transformer<String, String> split(String pattern) {
        LeftOver initialState = new LeftOver(null);
        Func3<LeftOver, String, Observer<String>, LeftOver> transition = new Func3<LeftOver, String, Observer<String>, LeftOver>() {

            @Override
            public LeftOver call(LeftOver leftOver, String s, Observer<String> observer) {
                String[] parts = s.split(pattern, -1);
                // prepend leftover to the first part
                if (leftOver.value != null)
                    parts[0] = leftOver.value + parts[0];

                // can emit all parts except the last part because it hasn't
                // been terminated by the pattern/end-of-stream yet
                for (int i = 0; i < parts.length - 1; i++)
                    observer.onNext(parts[i]);

                // we have to assign the last part as leftOver because we
                // don't know if it has been terminated yet
                return new LeftOver(parts[parts.length - 1]);
            }
        };

        Action2<LeftOver, Observer<String>> completionAction = new Action2<LeftOver, Observer<String>>() {

            @Override
            public void call(LeftOver leftOver, Observer<String> observer) {
                if (leftOver.value != null)
                    observer.onNext(leftOver.value);
                observer.onCompleted();
            }
        };
        return com.github.davidmoten.rx.Transformers.stateMachine(initialState, transition,
                completionAction);
    }

}
