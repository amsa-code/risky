package au.gov.amsa.craft.analyzer.wms;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import rx.internal.util.ScalarSynchronousObservable;

public class MergeTest {
    
    @Test
    public void testIsNotScalarSynchronousObservableAfterFlatMap() {
        Observable<Integer> o = Observable.just(1).flatMap(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer n) {
                return Observable.range(1, 100);
            }});
        Assert.assertFalse(o instanceof ScalarSynchronousObservable);
    }

}
