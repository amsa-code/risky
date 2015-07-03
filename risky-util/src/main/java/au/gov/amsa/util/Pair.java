package au.gov.amsa.util;

public class Pair<T, S> {

    private final T a;
    private final S b;

    public Pair(T a, S b) {
        this.a = a;
        this.b = b;
    }

    public static <T, S> Pair<T, S> create(T t, S s) {
        return new Pair<T, S>(t, s);
    }

    public T a() {
        return a;
    }

    public S b() {
        return b;
    }

    public T left() {
        return a;
    }

    public S right() {
        return b;
    }

}
