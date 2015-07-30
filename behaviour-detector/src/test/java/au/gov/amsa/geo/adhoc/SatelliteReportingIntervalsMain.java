package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.Pair;
import rx.Observable;

public class SatelliteReportingIntervalsMain {

    /**
     * 
     * Used this oracle query to get time diffs by mmsi for satellite reports
     * from cts.position.
     * 
     * <pre>
     * select mmsi, timeMs from 
     * (
     * select mmsi, EXTRACT( DAY    FROM diff ) * 24*60*60000
     *      + EXTRACT( HOUR   FROM diff ) *  60*60000
     *      + EXTRACT( MINUTE FROM diff ) *  60000
     *      + EXTRACT( SECOND FROM diff ) * 1000 timeMs from 
     * (
     * select mmsi, position_time - to_timestamp('1970-01-01', 'yyyy-mm-dd') diff from cts.position 
     * where position_time >= to_date('2015/06/28','yyyy/mm/dd') and position_time < to_date('2015/06/30','yyyy/mm/dd') 
     * and (source_detail like 'AISSat%' or source_detail like 'NORAIS%' or source_detail like 'rEV%')
     * )
     * ) order by mmsi, timeMs
     * </pre>
     * 
     * @param args
     */
    public static void main(String[] args) {

        Observable<Double> splits = Strings.from(new File("/home/dxm/times.txt"))
                //
                .compose(o -> Strings.split(o, "\n"))
                //
                .filter(line -> line.trim().length() > 0)
                //
                .map(line -> line.split("\t"))
                //
                // .doOnNext(items -> System.out.println(Arrays.asList(items)))
                //
                .map(items -> new Record(Long.parseLong(items[0]),
                        Long.parseLong(items[1]) / ((double) TimeUnit.HOURS.toMillis(1))))
                //
                .groupBy(record -> record.mmsi)
                //
                .flatMap(g -> g.buffer(2, 1)
                        //
                        .filter(list -> list.size() == 2)
                        // time diff
                        .map(list -> list.get(1).timeHrs - list.get(0).timeHrs))
                // sort
                .toSortedList()
                // flatten
                .flatMap(list -> Observable.from(list))
                //
                .cast(Double.class).cache();

        splits.reduce(Pair.create(0, 0.0),
                (p1, x) -> Pair.<Integer, Double> create(p1.a() + 1, x + p1.b()))
                //
                .map(pair -> pair.b() / pair.a())
                //
                .doOnNext(value -> System.out.println("average interval hours=" + value))
                //
                .subscribe();

        Observable<BucketCount> buckets = splits
                //
                .map(diff -> Math.floor(diff * 10) / 10.0)
                // collect into discrete interval buckets
                .collect(() -> new HashMap<Double, Integer>(), (map, x) -> {
                    if (map.get(x) == null)
                        map.put(x, 1);
                    else
                        map.put(x, map.get(x) + 1);
                })
                // sort keys
                .map(map -> new TreeMap<Double, Integer>(map))
                // flatten
                .flatMap(map -> Observable.from(map.entrySet()))
                // map to bucket
                .map(entry -> new BucketCount(entry.getKey(), entry.getValue()))
                //
                .scan(new BucketCount(0.0, 0),
                        (b1, b2) -> new BucketCount(b2.bucket, b1.count + b2.count))
                // cache
                .cache();
        double max = buckets.last().map(b -> b.count).toBlocking().single();
        buckets.doOnNext(b -> System.out.println(b.bucket + "\t" + b.count / max * 100)).count()
                .toBlocking().single();

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

    private static class BucketCount {
        final double bucket;
        final int count;

        BucketCount(double bucket, int count) {
            super();
            this.bucket = bucket;
            this.count = count;
        }

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
        return source != null && (source.startsWith("rEV") || source.startsWith("AISSat")
                || source.startsWith("NORAIS"));
    }

    private static boolean isNorwaySatellite(String source) {
        return source != null && (source.startsWith("AISSat") || source.startsWith("NORAIS"));
    }

    private static boolean isExactEarthSatellite(String source) {
        return source != null && source.startsWith("rEV");
    }
}
