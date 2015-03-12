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
public class Downsample implements Transformer<HasFix, HasFix> {

	private long maxTimeBetweenFixesMs;

	public Downsample(long minTimeBetweenFixesMs) {
		this.maxTimeBetweenFixesMs = minTimeBetweenFixesMs;
	}

	public static Downsample minTimeStep(long duration, TimeUnit unit) {
		return new Downsample(unit.toMillis(duration));
	}

	@Override
	public Observable<HasFix> call(Observable<HasFix> fixes) {
		return fixes.scan(new Func2<HasFix, HasFix, HasFix>() {
			@Override
			public HasFix call(HasFix latest, HasFix fix) {
				if (fix.fix().getTime() < latest.fix().getTime())
					throw new RuntimeException("not in ascending time order!");
				else if (fix.fix().getMmsi() != latest.fix().getMmsi())
					throw new RuntimeException("can only downsample a single vessel");
				else if (fix.fix().getTime() - latest.fix().getTime() >= maxTimeBetweenFixesMs)
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
