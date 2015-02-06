package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.operators.OperatorUnsubscribeEagerly;
import com.github.davidmoten.rx.slf4j.Logging;
import com.github.davidmoten.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;

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
		Preconditions.checkNotNull(input);
		Preconditions.checkNotNull(output);
		Preconditions.checkNotNull(pattern);
		Preconditions.checkNotNull(unit);

		return Observable
		// get the files matching the pattern from the directory
				.from(Files.find(input, pattern))
				// replace the file with a downsampled version
				.flatMap(new Func1<File, Observable<Integer>>() {

					@Override
					public Observable<Integer> call(File file) {
						File outputFile = rebase(file, input, output);
						outputFile.getParentFile().mkdirs();
						return BinaryFixes.from(file)
								// ensure file is closed in case we want to
								// rewrite downstream
								.lift(OperatorUnsubscribeEagerly
										.<Fix> instance())
								// to list
								.toList()
								// flatten
								.flatMapIterable(
										Functions.<List<Fix>> identity())
								// downsample the sorted fixes
								.compose(Downsample.minTimeStep(duration, unit))
								// make into a list again
								.toList()
								// replace the file with sorted fixes
								.doOnNext(writeFixes(outputFile))
								// count the fixes
								.count()
								// log completion of rewrite
								.lift(Logging
										.<Integer> logger()
										.prefix("downsampled by " + duration
												+ " " + unit + " file=" + file
												+ " ").log());
					}
				});

	}

	@VisibleForTesting
	static File rebase(File file, File existingParent, File newParent) {
		if (file.getAbsolutePath().equals(existingParent.getAbsolutePath()))
			return newParent;
		else
			return new File(rebase(file.getParentFile(), existingParent,
					newParent), file.getName());
	}

	private static Action1<List<Fix>> writeFixes(final File file) {
		return new Action1<List<Fix>>() {
			@Override
			public void call(List<Fix> list) {
				BinaryFixesWriter.writeFixes(list, file, false);
			}
		};
	}
}
