package au.gov.amsa.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import rx.Observable;

public class StateMachineExampleTest {

    @Test
    public void testForStackOverflow() {
        Observable<Integer> a = Observable.just(1, 1, 2, 2, 2, 3);
        State<Integer> initial = new State<Integer>(Collections.emptyList(), Optional.empty(),
                false);
        List<List<Integer>> lists = a.materialize()
                // accumulate lists and uses onCompleted notification to emit
                // left overs when source completes
                .scan(initial,
                        (state, notification) -> {
                            if (notification.isOnCompleted()) {
                                return new State<>(null, state.value, true);
                            } else if (notification.isOnError())
                                throw new RuntimeException(notification.getThrowable());
                            else if (state.list.size() == 0) {
                                return new State<>(Arrays.asList(notification.getValue()), Optional
                                        .empty(), false);
                            } else if (partitionId(notification.getValue()) == partitionId(state.list
                                    .get(0))) {
                                List<Integer> list = new ArrayList<>();
                                list.addAll(state.list);
                                list.add(notification.getValue());
                                return new State<>(list, Optional.empty(), false);
                            } else if (state.value.isPresent()) {
                                if (partitionId(state.value.get()) == partitionId(notification
                                        .getValue())) {
                                    return new State<>(Arrays.asList(state.value.get(),
                                            notification.getValue()), Optional.empty(), false);
                                } else {
                                    return new State<>(Arrays.asList(state.value.get()), Optional
                                            .of(notification.getValue()), false);
                                }
                            } else {
                                return new State<>(state.list,
                                        Optional.of(notification.getValue()), false);
                            }
                        })
                // emit lists from state
                .flatMap(state -> {
                    if (state.completed) {
                        if (state.value.isPresent())
                            return Observable.just(Arrays.asList(state.value.get()));
                        else
                            return Observable.empty();
                    } else if (state.value.isPresent()) {
                        return Observable.just(state.list);
                    } else {
                        return Observable.empty();
                    }
                })
                // get as a list of lists to check
                .toList().toBlocking().single();
        assertEquals(Arrays.asList(Arrays.asList(1, 1), Arrays.asList(2, 2, 2), Arrays.asList(3)),
                lists);
    }

    private static int partitionId(Integer n) {
        return n;
    }

    private static final class State<T> {
        final List<T> list;
        final Optional<T> value;
        final boolean completed;

        State(List<T> list, Optional<T> value, boolean completed) {
            this.list = list;
            this.value = value;
            this.completed = completed;
        }
    }

}
