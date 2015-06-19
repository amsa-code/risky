package au.gov.amsa.util.rx;

import java.util.ArrayList;
import java.util.List;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Buffers items into lists that can overlap but not more than two at a time.
 * The commencement of the next buffer is signalled by a change in the value of
 * {@code startFunction} and the finish of a buffer is signalled by a change in
 * the value of the {@code whileFunction}.
 *
 * @param <T>
 *            generic type
 */
public class OperatorDynamicBuffer<T> implements Operator<List<T>, T> {

    private static final Object NOT_SET = new Object();
    private static final Object NULL_SENTINEL = new Object();
    private final Func1<T, ?> startFunction;
    private final Func1<T, ?> whileFunction;

    /**
     * Constructor.
     * 
     * @param startFunction
     *            a change in the value of this function signals the start of a
     *            new buffer
     * @param whileFunction
     *            a change in the value of this function signals the finish of a
     *            buffer
     */
    public OperatorDynamicBuffer(Func1<T, ?> startFunction, Func1<T, ?> whileFunction) {
        this.startFunction = startFunction;
        this.whileFunction = whileFunction;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super List<T>> child) {
        return new Subscriber<T>(child) {

            private List<T> list;
            private List<T> list2;
            private Object lastStartValue = NOT_SET;
            private Object lastWhileValue = NOT_SET;

            @Override
            public void onCompleted() {
                if (list != null && !isUnsubscribed())
                    child.onNext(list);
                if (list2 != null && !isUnsubscribed())
                    child.onNext(list2);
                if (!isUnsubscribed())
                    child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (list == null)
                    list = new ArrayList<T>();
                Object startValue = replaceNull(startFunction.call(t));
                Object whileValue = replaceNull(whileFunction.call(t));
                if (lastWhileValue == NOT_SET) {
                    list.add(t);
                    request(1);
                } else {
                    if (lastWhileValue.equals(whileValue)) {
                        list.add(t);
                        request(1);
                    } else {
                        // emit list 1
                        child.onNext(list);
                        list = null;
                    }
                    if (lastStartValue != NOT_SET && !lastStartValue.equals(startValue)) {
                        // startFunction value has changed so we are ready to
                        // start another buffer
                        if (list2 == null) {
                            list2 = new ArrayList<T>();
                        } else {
                            throw new RuntimeException("unexpected");
                        }
                        list2.add(t);
                    }
                    if (list == null && list2 != null) {
                        // move second list into first position
                        list = list2;
                        list2 = null;
                    }
                }
                lastStartValue = startValue;
                lastWhileValue = whileValue;
            }
        };
    }

    private static Object replaceNull(Object o) {
        if (o == null)
            return NULL_SENTINEL;
        else
            return o;
    }
}
