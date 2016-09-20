package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.grumpy.core.Position;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;
import rx.Observable;

public class HeadingCourseDiffMain {

    private static final Logger log = LoggerFactory.getLogger(CountCrossingsIntoRegionMain.class);

    public static void main(String[] args) {

        Pattern pattern = Pattern.compile(".*\\.track");
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern);
        // files.addAll(Files.find(new
        // File("/media/an/binary-fixes-5-minute/2013"), pattern));
        // files.addAll(Files.find(new
        // File("/media/an/binary-fixes-5-minute/2014"), pattern));
        // files.addAll(Files.find(new
        // File("/media/an/binary-fixes-5-minute/2015"), pattern));
        log.info("files=" + files.size());

        ConcurrentHashMap<Integer, Long> map = new ConcurrentHashMap<>();
        int count = Observable.from(files) //
                // .doOnNext(System.out::println) //
                // .doOnNext(System.out::println)
                .concatMap(file -> BinaryFixes.from(file)
                        .filter(f -> f.courseOverGroundDegrees().isPresent()
                                && f.headingDegrees().isPresent())
                        .map(f -> Math.abs(Position.getBearingDifferenceDegrees(
                                f.courseOverGroundDegrees().get(), f.headingDegrees().get()))) //
                        .doOnNext(x -> {
                            int bucket = (int) Math.round(x);
                            map.compute(bucket, (n, num) -> num == null ? 1 : num + 1);
                        })//
        ) //
                .count().toBlocking().single();

        System.out.println("count=" + count);
        for (Integer key : new TreeSet<Integer>(map.keySet())) {
            System.out.println(key + "\t" + map.get(key));
        }
    }

}
