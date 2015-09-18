package au.gov.amsa.risky.format;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;

import au.gov.amsa.util.Files;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

public final class Formats {

    private static final Logger log = LoggerFactory.getLogger(Formats.class);

    public static Observable<Integer> transform(final File input, final File output,
            Pattern pattern, final Transformer<HasFix, HasFix> transformer,
            final Action2<List<HasFix>, File> fixesWriter, final Func1<String, String> renamer) {
        Preconditions.checkNotNull(input);
        Preconditions.checkNotNull(output);
        Preconditions.checkNotNull(pattern);
        Preconditions.checkNotNull(transformer);
        final List<File> files = Files.find(input, pattern);
        long n = 0;
        for (File file : files)
            n += file.length();
        final long totalSizeBytes = n;
        log.info("transforming " + new DecimalFormat("0.000").format(totalSizeBytes / 1000000.0)
                + "MB");
        final Action1<File> logger = new Action1<File>() {
            final AtomicInteger count = new AtomicInteger();
            final long startTime = System.currentTimeMillis();
            final AtomicLong size = new AtomicLong();

            @Override
            public void call(File f) {
                long t = System.currentTimeMillis();
                int n = count.incrementAndGet();
                long bytes = size.getAndAdd(f.length());
                double timeToFinishMins;
                if (n > 1) {
                    timeToFinishMins = (t - startTime) / (double) (bytes) * (totalSizeBytes - bytes)
                            / 1000.0 / 60.0;
                } else
                    timeToFinishMins = -1;
                DecimalFormat df = new DecimalFormat("0.000");
                log.info("transforming " + n + " of " + files.size() + ":" + f + ", sizeMB="
                        + df.format(f.length() / 1000000.0) + ", finish in mins="
                        + df.format(timeToFinishMins));
            }
        };

        log.info("converting " + files.size() + " files" + " in " + input);
        return Observable
                // get the files matching the pattern from the directory
                .from(files)
                // replace the file with a transformed version
                .flatMap(file -> {
                    final File outputFile = rebase(file, input, output);
                    outputFile.getParentFile().mkdirs();
                    logger.call(file);
                    return BinaryFixes.from(file, true, BinaryFixesFormat.WITHOUT_MMSI)
                            // to list
                            .toList()
                            // flatten
                            .flatMapIterable(Functions.<List<Fix>> identity())
                            // transform the fixes
                            .compose(transformer)
                            // make into a list again
                            .toList()
                            // replace the file with sorted fixes
                            .doOnNext(list -> {
                        File f = new File(outputFile.getParentFile(),
                                renamer.call(outputFile.getName()));
                        fixesWriter.call(list, f);
                    })
                            // count the fixes
                            .count();
                });

    }

    @VisibleForTesting
    static File rebase(File file, File existingParent, File newParent) {
        if (file.getAbsolutePath().equals(existingParent.getAbsolutePath()))
            return newParent;
        else
            return new File(rebase(file.getParentFile(), existingParent, newParent),
                    file.getName());
    }

}
