package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.operators.OperatorUnsubscribeEagerly;
import com.github.davidmoten.rx.slf4j.Logging;
import com.github.davidmoten.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;

public final class Formats {

	public static Observable<Integer> transform(final File input,
			final File output, Pattern pattern,
			final Transformer<Fix, Fix> transformer,
			final Action2<List<Fix>, File> fixesWriter,
			final Func1<String, String> renamer) {
		Preconditions.checkNotNull(input);
		Preconditions.checkNotNull(output);
		Preconditions.checkNotNull(pattern);
		Preconditions.checkNotNull(transformer);

		return Observable
		// get the files matching the pattern from the directory
				.from(Files.find(input, pattern))
				// replace the file with a downsampled version
				.flatMap(new Func1<File, Observable<Integer>>() {

					@Override
					public Observable<Integer> call(File file) {
						final File outputFile = rebase(file, input, output);
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
								.compose(transformer)
								// make into a list again
								.toList()
								// replace the file with sorted fixes
								.doOnNext(new Action1<List<Fix>>() {
									@Override
									public void call(List<Fix> list) {
										File file = new File(outputFile
												.getParentFile(), renamer
												.call(outputFile.getName()));
										fixesWriter.call(list, file);
									}
								})// writeFixes(outputFile)
									// count the fixes
								.count()
								// log completion of rewrite
								.lift(Logging
										.<Integer> logger()
										.prefix("transformed file=" + file
												+ " to " + outputFile).log());
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

}
