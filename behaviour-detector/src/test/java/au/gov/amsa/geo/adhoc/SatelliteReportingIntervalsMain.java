package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.streams.Strings;

import com.github.davidmoten.rx.Transformers;

public class SatelliteReportingIntervalsMain {

    public static void main(String[] args) {
        Strings.from(new File("/home/dxm/temp.txt"))
                //
                .compose(o -> Strings.split(o, "\n"))
                //
                .filter(line -> line.trim().length() > 0)
                //
                .map(line -> line.split("\t"))
                //
                .doOnNext(items -> System.out.println(Arrays.asList(items)))
                //
                .map(items -> new Record(Long.parseLong(items[0]), Long.parseLong(items[1])
                        / ((double) TimeUnit.HOURS.toMillis(1))))
                //
                .groupBy(record -> record.mmsi)
                //
                .flatMap(
                        g -> g.buffer(2)
                                //
                                .filter(list -> list.size() == 2)
                                // time diff
                                .map(list -> list.get(1).timeHrs - list.get(0).timeHrs)
                                .doOnNext(diff -> System.out.println("diff=" + diff))
                                .map(diff -> Math.round(diff * 100))).cast(Long.class)
                .groupBy(diff -> diff)
                .flatMap(g -> g.compose(Transformers.<Long> mapWithIndex()).takeLast(1))
                .collect(() -> new HashMap<Long, Integer>(), (m, x) -> {
                    if (m.get(x.value()) == null)
                        m.put(x.value(), 0);
                    m.put(x.value(), m.get(x.value()) + 1);
                }).doOnNext(System.out::println)
                //
                .count().toBlocking().single();
        System.exit(0);

        File file = new File("/home/dxm/2015-06-29.txt.gz");
        Streams.nmeaFromGzip(file)
        // to AisMessage
                .compose(o -> Streams.extractMessages(o))
                // filter on positions
                .filter(m -> m.message() instanceof AisPosition)
                //
                .filter(m -> isNorwaySatellite(m.message().getSource()))
                // group by mmsi
                .groupBy(m -> ((AisPosition) m.message()).getMmsi())
                // calculate intervals
                .flatMap(g -> g.toSortedList())
                // log
                .doOnNext(System.out::println)
                // count
                .count()
                //
                .toBlocking().single();
    }

    private static class Record {
        final long mmsi;
        final double timeHrs;

        Record(long mmsi, double timeHrs) {
            super();
            this.mmsi = mmsi;
            this.timeHrs = timeHrs;
        }

    }

    private static boolean isSatellite(String source) {
        return source != null
                && (source.startsWith("rEV") || source.startsWith("AISSat") || source
                        .startsWith("NORAIS"));
    }

    private static boolean isNorwaySatellite(String source) {
        return source != null && (source.startsWith("AISSat") || source.startsWith("NORAIS"));
    }

    private static boolean isExactEarthSatellite(String source) {
        return source != null && source.startsWith("rEV");
    }
}
