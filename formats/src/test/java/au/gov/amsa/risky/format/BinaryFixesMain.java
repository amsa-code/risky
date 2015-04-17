package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import rx.Observable;
import rx.schedulers.Schedulers;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.slf4j.Logging;

public class BinaryFixesMain {

    public static void main(String[] args) {
        // perform a speed test for loading BinaryFixes from disk

        FixImpl.validate = false;
        final ConcurrentHashMap<Long, List<FixImpl>> map = new ConcurrentHashMap<Long, List<FixImpl>>();
        // -downsample-5-mins
        List<File> files = Files.find(
                new File("/media/an/binary-fixes/2014-year-downsample-5-mins"),
                Pattern.compile(".*\\.track"));
        long t = System.currentTimeMillis();
        long count = Observable
        // list files
                .from(files)
                // share the load between processors
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors()))
                // count each file asynchronously
                .flatMap(
                        list -> {
                            return Observable.from(list)
                                    .concatMap(file -> BinaryFixes.from(file).countLong())
                                    // schedule
                                    .subscribeOn(Schedulers.computation());
                        })
                // total counts
                .scan(0L, (a, b) -> a + b)
                // log count so far
                .lift(Logging.<Long> logger().showCount().prefix("records=").showMemory()
                        .every(1000).log())
                // get last count (total)
                .last()
                // block and get
                .toBlocking().single();
        long elapsed = System.currentTimeMillis() - t;
        System.out.println("Map size = " + map.size());
        System.out.println("Total records = " + count + ", numPerSecond=" + count * 1000.0
                / elapsed + ", timeMs=" + elapsed);
    }

}
