package au.gov.amsa.ais.rx;

import java.io.IOException;
import java.io.OutputStream;

import rx.Observable;
import rx.functions.Func1;

public class RxUtil {

    public static <T> Func1<T, T> println(final OutputStream out) {
        return t -> {
            try {
                out.write(t.toString().getBytes());
                out.write('\n');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return t;
        };
    }

    public static <T> Func1<T, T> println() {
        return println(System.out);
    }

    public static <T> void print(Observable<T> o) {
        o.materialize().toBlocking().forEach(System.out::println);
    }

    @SuppressWarnings("unchecked")
    public static <T> Observable<T> concatButIgnoreFirstSequence(Observable<?> o1, Observable<T> o2) {
        return Observable.concat((Observable<T>) o1.filter(com.github.davidmoten.rx.Functions
                .<Object> alwaysFalse()), o2);
    }

}
