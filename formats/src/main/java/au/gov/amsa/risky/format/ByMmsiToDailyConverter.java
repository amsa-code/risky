package au.gov.amsa.risky.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.slf4j.Logging;

import au.gov.amsa.util.Files;
import rx.Observable;
import rx.schedulers.Schedulers;

public class ByMmsiToDailyConverter {

    private static final Logger log = LoggerFactory.getLogger(ByMmsiToDailyConverter.class);

    public static void convert(File input, File output) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        output.mkdirs();
        try {
            FileUtils.cleanDirectory(output);
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        {
            List<File> files = Files.find(input, Pattern.compile(".*\\.track"));
            System.out.println("found " + files.size() + " files");
            int bufferSize = 1000;
            Observable.from(files)
                    //
                    .flatMap(file -> BinaryFixes.from(file, true))
                    //
                    .lift(Logging.<Fix> logger().showMemory().every(1000000)
                            .showCount("recordsMillions").log())
                    //
                    .groupBy(fix -> dtf.format(Instant.ofEpochMilli(fix.time())))
                    //
                    .flatMap(g -> g.buffer(bufferSize))
                    //
                    .doOnNext(list -> {
                        File file = new File(output,
                                dtf.format(Instant.ofEpochMilli(list.get(0).time())) + ".fix");
                        try (OutputStream os = new FileOutputStream(file, true)) {
                            for (Fix fix : list) {
                                BinaryFixes.write(fix, os, BinaryFixesFormat.WITH_MMSI);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).count().toBlocking().single();
        }

    }

    public static void sort(File output) {
        // now sort the data in each output file by time and rewrite
        List<File> files = Files.find(output, Pattern.compile(".*\\.fix"));
        Observable.from(files)
                //
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors()))
                //
                .flatMap(list -> Observable.from(list)
                        //
                        .doOnNext(file -> sortFixFile(file)).subscribeOn(Schedulers.computation()))
                .count().toBlocking().single();
    }

    static void sortFixFile(File file) {
        log.info("sorting " + file.getName() + ", size="
                + new DecimalFormat("0.00").format(file.length() / 1024.0 / 1024.0));

        try {
            File temp = new File(file.getParent(), file.getName() + ".tmp");
            temp.delete();
            ArrayList<Fix> fixes = BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI)
                    .collect(() -> new ArrayList<Fix>(20_000_000), (list, fix) -> list.add(fix))
                    .toBlocking().single();
            Collections.sort(fixes, (a, b) -> Long.compare(a.time(), b.time()));
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(temp))) {
                for (Fix fix : fixes) {
                    BinaryFixes.write(fix, os, BinaryFixesFormat.WITH_MMSI);
                }
            }
            file.delete();
            temp.renameTo(file);
            log.info("sorted");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}