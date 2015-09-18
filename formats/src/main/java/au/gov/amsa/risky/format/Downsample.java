package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.github.davidmoten.rx.Functions;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action2;
import rx.functions.Func1;

/**
 * Assumes input stream is in time order.
 */
public class Downsample<T extends HasFix> implements Transformer<T, T> {

    private final long minTimeBetweenFixesMs;
    private final Func1<T, Boolean> selector;

    public Downsample(long minTimeBetweenFixesMs, Func1<T, Boolean> selector) {
        this.minTimeBetweenFixesMs = minTimeBetweenFixesMs;
        this.selector = selector;
    }

    public static <T extends HasFix> Downsample<T> minTimeStep(long duration, TimeUnit unit) {
        return new Downsample<T>(unit.toMillis(duration), Functions.<T> alwaysFalse());
    }

    public static <T extends HasFix> Downsample<T> minTimeStep(long duration, TimeUnit unit,
            Func1<T, Boolean> selector) {
        return new Downsample<T>(unit.toMillis(duration), selector);
    }

    @Override
    public Observable<T> call(Observable<T> fixes) {
        Observable<T> result = fixes.scan((latest, fix) -> {
            if (fix.fix().mmsi() != latest.fix().mmsi())
                throw new RuntimeException("can only downsample a single vessel");
            else if (fix.fix().time() < latest.fix().time())
                throw new RuntimeException("not in ascending time order!");
            else if (fix.fix().time() - latest.fix().time() >= minTimeBetweenFixesMs
                    || selector.call(fix))
                return fix;
            else
                return latest;
        });
        if (minTimeBetweenFixesMs > 0)
            // throw away repeats
            result = result.distinctUntilChanged(f -> f.fix().time());
        return result;
    }

    public static Observable<Integer> downsample(final File input, final File output,
            Pattern pattern, final long duration, final TimeUnit unit) {
        return Formats.transform(input, output, pattern, Downsample.minTimeStep(duration, unit),
                FIXES_WRITER_WITHOUT_MMSI, Functions.<String> identity());
    }

    private static Action2<List<HasFix>, File> FIXES_WRITER_WITHOUT_MMSI = (list, file) -> {
        BinaryFixesWriter.writeFixes(list, file, false, false, BinaryFixesFormat.WITHOUT_MMSI);
    };

}
