package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.slf4j.Logging;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.identity.MmsiValidator2;
import rx.Observable;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class AdHocMain {

    public static void main(String[] args) throws IOException {
        long start = ZonedDateTime
                .from(DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-13T00:00:00Z")).toEpochSecond()
                * 1000;
        long finish = ZonedDateTime
                .from(DateTimeFormatter.ISO_DATE_TIME.parse("2014-05-27T00:00:00Z")).toEpochSecond()
                * 1000;

        Pattern pattern = Pattern.compile(".*\\.track");
        PrintStream out = new PrintStream("target/output.txt");
        out.println("mmsi\ttime\tlat\tlong\tcourse\tspeedKnots");
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern);
        Observable.from(files)
                //
                .flatMap(file -> extract(file, start, finish).subscribeOn(Schedulers.computation()))
                // only valid mmsi
                .filter(fix -> MmsiValidator2.INSTANCE.isValid(fix.mmsi()))
                //
                .map(f -> String.format("%s\t%s\t%s\t%s\t%s\t%s", f.mmsi(),
                        formatDateTime(f.time()), f.lat(), f.lon(),
                        get(f.courseOverGroundDegrees()), get(f.speedOverGroundKnots())))
                //
                // .doOnNext(System.out::println)
                //
                .doOnNext(out::println)
                //
                .lift(Logging.<String> logger().showCount().every(10000).log())
                //
                .count()
                //
                .doOnTerminate(out::close)
                //
                .toBlocking().single();
    }

    private static Observable<Fix> extract(File file, long start, long finish) {
        return BinaryFixes.from(file, true)
                .filter(fix -> fix.time() >= start && fix.time() <= finish && fix.lat() >= -23
                        && fix.lat() <= 10 && fix.lon() >= 113 && fix.lon() <= 156)
                .toSortedList(ascendingTime).flatMapIterable(Functions.identity());
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static String formatDateTime(long time) {
        return dtf.format(Instant.ofEpochMilli(time).atZone(ZoneOffset.UTC));
    }

    private static final Func2<Fix, Fix, Integer> ascendingTime = (a, b) -> Long.compare(a.time(),
            b.time());

    private static String get(Optional<Float> x) {
        if (x.isPresent())
            return x.get().toString();
        else
            return "";
    }
}