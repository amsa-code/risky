package au.gov.amsa.risky.format;

import rx.Observable.Transformer;

public final class Transformers {

    public static <T> Transformer<T, T> identity() {
        return o -> o;
    }

}
