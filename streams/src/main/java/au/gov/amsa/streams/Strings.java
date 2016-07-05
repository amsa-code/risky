package au.gov.amsa.streams;

import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;

import rx.Observable;
import rx.functions.Func1;

/**
 * Utilities for stream processing of lines of text from
 * <ul>
 * <li>sockets</li>
 * </ul>
 */
public final class Strings {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * Returns null if input is null otherwise returns input.toString().trim().
     */
    public static Func1<Object, String> TRIM = new Func1<Object, String>() {

        @Override
        public String call(Object input) {
            if (input == null)
                return null;
            else
                return input.toString().trim();
        }
    };

    public static Observable<String> from(final Reader reader, final int size) {
        return new OnSubscribeReader(reader, size).toObservable();
        // return StringObservable.from(reader, size);
    }

    public static Observable<String> from(Reader reader) {
        return from(reader, 8192);
    }

    public static Observable<String> lines(Reader reader) {
        return from(reader, 8192).compose(com.github.davidmoten.rx.Transformers.split("\n"));
    }

    public static Observable<String> lines(File file) {
        return from(file).compose(com.github.davidmoten.rx.Transformers.split("\n"));
    }

    public static Observable<String> from(File file) {
        return from(file, DEFAULT_CHARSET);
    }

    public static Observable<String> split(Observable<String> o, String pattern) {
        return o.compose(com.github.davidmoten.rx.Transformers.split(pattern));
    }

    public static Observable<String> from(final File file, final Charset charset) {
        return com.github.davidmoten.rx.Strings.from(file, charset);
    }

}
