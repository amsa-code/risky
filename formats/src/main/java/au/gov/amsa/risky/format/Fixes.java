package au.gov.amsa.risky.format;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func2;

public final class Fixes {

    public static <T extends Fix> Transformer<T, T> ignoreOutOfOrderFixes(final boolean enabled) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.scan(new Func2<T, T, T>() {
                    @Override
                    public T call(T a, T b) {
                        if (!enabled || b.time() > a.time())
                            return b;
                        else
                            return a;
                    }
                }).distinctUntilChanged();
            }
        };
    }

}
