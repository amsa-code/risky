import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class BackpressureGcMain {

    public static void main(String... args) throws InterruptedException {
        Observable.range(0, 20)
                // batch the filenames so we don't overload merge
                .buffer(Runtime.getRuntime().availableProcessors() - 2)
                .map(new Func1<List<Integer>, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(List<Integer> list) {
                        return Observable.from(list);
                    }
                }).concatMap(new Func1<Observable<Integer>, Observable<Pointer>>() {
                    @Override
                    public Observable<Pointer> call(Observable<Integer> n) {
                        return Observable
                        // integers for ever
                                .range(0, Integer.MAX_VALUE)
                                // use up some heap
                                .map(new Func1<Integer, Pointer>() {
                                    @Override
                                    public Pointer call(Integer m) {
                                        // use up some gc for every item
                                        Pointer pointer = new Pointer(null);
                                        for (int i = 0; i < 1000; i++)
                                            pointer = new Pointer(pointer);
                                        
                                        return pointer;
                                    }
                                })
                                // async
                                .subscribeOn(Schedulers.computation());
                    }
                }).map(new Func1<Pointer, Boolean>() {

                    @Override
                    public Boolean call(Pointer pointer) {
                        if (pointer == null)
                            return true;
                        else
                            return false;
                    }
                }).subscribe();
        Thread.sleep(100000000L);
    }

    private static class Pointer {
        private Object o;

        Pointer(Object o) {
            this.o = o;
        }
    }

}
