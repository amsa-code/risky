package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action2;
import rx.functions.Func2;

import com.github.davidmoten.rx.Functions;

/**
 * Assumes input stream is in time order.
 */
public class Downsample implements Transformer<Fix, Fix> {

	private long maxTimeBetweenFixesMs;

	public Downsample(long minTimeBetweenFixesMs) {
		this.maxTimeBetweenFixesMs = minTimeBetweenFixesMs;
	}

	public static Downsample minTimeStep(long duration, TimeUnit unit) {
		return new Downsample(unit.toMillis(duration));
	}

	@Override
	public Observable<Fix> call(Observable<Fix> fixes) {
		return fixes.scan(new Func2<Fix, Fix, Fix>() {
			@Override
			public Fix call(Fix latest, Fix fix) {
				if (fix.getTime() < latest.getTime())
					throw new RuntimeException("not in ascending time order!");
				else if (fix.getMmsi() != latest.getMmsi())
					throw new RuntimeException(
							"can only downsample a single vessel");
				else if (fix.getTime() - latest.getTime() >= maxTimeBetweenFixesMs)
					return fix;
				else
					return latest;
			}
		})
		// throw away repeats
				.distinct();
	}

	public static Observable<Integer> downsample(final File input,
			final File output, Pattern pattern, final long duration,
			final TimeUnit unit) {
		return Formats.transform(input, output, pattern,
				Downsample.minTimeStep(duration, unit), FIXES_WRITER,
				Functions.<String> identity());
	}

	private static Action2<List<Fix>, File> FIXES_WRITER = new Action2<List<Fix>, File>() {
		@Override
		public void call(List<Fix> list, File file) {
			BinaryFixesWriter.writeFixes(list, file, false, false);
		}
	};

}
