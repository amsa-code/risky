package au.gov.amsa.util.rx;

import org.junit.Ignore;
import org.junit.Test;

import rx.Observable;

public class OperatorDynamicBufferTest {

    @Test
    @Ignore
    public void test() {
        Observable
                .range(1, 20)
                .lift(new OperatorDynamicBuffer<Integer>(i -> (i - 1) % 3 == 0, i -> Math.max(0,
                        (i - 2) / 3))).forEach(System.out::println);
    }
}
