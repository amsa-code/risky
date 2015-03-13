package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;

import com.github.davidmoten.rx.Functions;

/**
 * Assumes input stream is in time order.
 */
public class Downsample<T extends HasFix> implements Transformer<T, T> {

	private long maxTimeBetweenFixesMs;
	private Func1<T, Boolean> selector;

	public Downsample(long minTimeBetweenFixesMs, Func1<T, Boolean> selector) {
		this.maxTimeBetweenFixesMs = minTimeBetweenFixesMs;
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
		return fixes.scan(new Func2<T, T, T>() {
			@Override
			public T call(T latest, T fix) {
				if (fix.fix().getTime() < latest.fix().getTime())
					throw new RuntimeException("not in ascending time order!");
				else if (fix.fix().getMmsi() != latest.fix().getMmsi())
					throw new RuntimeException("can only downsample a single vessel");
				else if (fix.fix().getTime() - latest.fix().getTime() >= maxTimeBetweenFixesMs
				        || selector.call(fix))
					return fix;
				else
					return latest;
			}
		})
		// throw away repeats
		        .distinct();
	}

	public static Observable<Integer> downsample(final File input, final File output,
	        Pattern pattern, final long duration, final TimeUnit unit) {
		return Formats.transform(input, output, pattern, Downsample.minTimeStep(duration, unit),
		        FIXES_WRITER, Functions.<String> identity());
	}

	private static Action2<List<HasFix>, File> FIXES_WRITER = new Action2<List<HasFix>, File>() {
		@Override
		public void call(List<HasFix> list, File file) {
			BinaryFixesWriter.writeFixes(list, file, false, false);
		}
	};

}
