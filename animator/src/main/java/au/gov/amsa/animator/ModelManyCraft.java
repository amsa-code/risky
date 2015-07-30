package au.gov.amsa.animator;

import static au.gov.amsa.geo.distance.EffectiveSpeedChecker.effectiveSpeedOk;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.util.MapWithIndex;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.geo.distance.EffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.schedulers.Schedulers;

public class ModelManyCraft implements Model {

    private final FixesSubscriber subscriber;
    private final int fixesPerModelStep;
    private volatile long stepNumber;

    public ModelManyCraft(Observable<Fix> fixes, int fixesPerModelStep) {
        this.fixesPerModelStep = fixesPerModelStep;
        this.subscriber = new FixesSubscriber();
        Observable<Fix> source = fixes
                // cache for repeat
                .cache()
                //
                .onBackpressureBuffer()
                //
                .doOnCompleted(() -> subscriber.reset())
                // repeat stream
                .repeat()
                // log
                .doOnCompleted(() -> {
                    System.out.println("completed");
                });
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long stepNumber) {
        this.stepNumber = stepNumber;
        subscriber.requestMore(fixesPerModelStep);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Long, Collection<Fix>> recent() {
        return (Map<Long, Collection<Fix>>) ((Map<Long, ?>) subscriber.queues);
    }

    @Override
    public long stepNumber() {
        return stepNumber;
    }

    private static class FixesSubscriber extends Subscriber<Fix> {

        private final ConcurrentHashMap<Long, Queue<Fix>> queues = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, Fix> lastFix = new ConcurrentHashMap<>();
        private final int maxSize = 10;
        private final SegmentOptions options = SegmentOptions.builder().build();

        synchronized void reset() {
            queues.clear();
            lastFix.clear();
        }

        @Override
        public void onStart() {
            request(0);
        }

        public void requestMore(long n) {
            request(n);
        }

        @Override
        public void onCompleted() {
            System.out.println("finished");
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onNext(Fix f) {
            Queue<Fix> queue = queues.computeIfAbsent(f.mmsi(),
                    mmsi -> new ConcurrentLinkedQueue<Fix>());
            if (queue.size() == maxSize)
                queue.poll();
            Fix last = lastFix.get(f.mmsi());
            if (last == null || f.time() >= last.time() + 600000 && effectiveSpeedOk(last.time(),
                    last.lat(), last.lon(), f.time(), f.lat(), f.lon(), options)) {
                queue.add(f);
                lastFix.put(f.mmsi(), f);
            }
        }
    }

    private static Func1<List<Fix>, Fix> extrapolateToNext(long startTime, long intervalMs) {
        return list -> {
            if (list.size() == 0)
                throw new RuntimeException("unexpected");
            else {
                Fix a = list.get(0);

                if (list.size() == 1) {
                    Fix b = a;
                    long t = nextIntervalStartTime(startTime, intervalMs, a);
                    return new FixImpl(b.mmsi(), b.lat(), b.lon(), t, b.latencySeconds(),
                            b.source(), b.navigationalStatus(), b.speedOverGroundKnots(),
                            b.courseOverGroundDegrees(), b.headingDegrees(), b.aisClass());
                } else {
                    Fix b = list.get(1);
                    long t = nextIntervalStartTime(startTime, intervalMs, b);
                    if (EffectiveSpeedChecker.effectiveSpeedOk(a.time(), a.lat(), a.lon(), b.time(),
                            b.lat(), b.lon(), SegmentOptions.getDefault())) {
                        FixImpl c = new FixImpl(b.mmsi(), b.lat(), b.lon(), t, b.latencySeconds(),
                                b.source(), b.navigationalStatus(), b.speedOverGroundKnots(),
                                b.courseOverGroundDegrees(), b.headingDegrees(), b.aisClass());
                        return c;
                    } else
                        return new FixImpl(b.mmsi(), b.lat(), b.lon(), t, b.latencySeconds(),
                                b.source(), b.navigationalStatus(), b.speedOverGroundKnots(),
                                b.courseOverGroundDegrees(), b.headingDegrees(), b.aisClass());
                }
            }
        };
    }

    private static long nextIntervalStartTime(long startTime, long intervalMs, Fix a) {
        return ((a.time() - startTime) / intervalMs + 1) * intervalMs + startTime;
    }

    public static void main(String[] args) {
        Observable.range(1, 10).groupBy(n -> n % 2).flatMap(g -> g.map(t -> g.getKey() + ":" + t))
                .subscribe(System.out::println);
        // System.exit(0);

        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file));
        final long startTime = 1391212800000L;
        System.out.println(new Date(startTime));

        final long intervalMs = TimeUnit.MINUTES.toMillis(5);

        source.buffer(1000000).compose(MapWithIndex.<List<Fix>> instance()).take(1)
                //
                .concatMap(buffer -> Observable.from(buffer.value())
                        // sort by time
                        .toSortedList((a, b) -> (((Long) a.time()).compareTo(b.time())))
                        // flatten
                        .concatMap(x -> Observable.from(x))
                        // group by timestep
                        .groupBy(fix -> (fix.time() - startTime) / intervalMs)
                        //
                        .flatMap(
                                // within each timestep group by mmsi
                                g -> g.groupBy(fix -> fix.mmsi())
                                        //
                                        .flatMap(g2 -> // take the last
                                                       // two at the end
                                                       // of the timestep
        g2.takeLast(2)
                // convert to a
                // list of 1 or
                // 2 items
                .toList()
                // predict
                // position at
                // end of
                // timestep
                .map(extrapolateToNext(startTime, intervalMs))))).cast(Fix.class)
                // sort by time
                .toSortedList((a, b) -> (((Long) a.time()).compareTo(b.time())))
                // flatten
                .flatMapIterable(UtilityFunctions.identity())
                // to string
                .map(fix -> new Date(fix.time()) + " " + fix)
                // go
                .subscribe(System.out::println);
    }
}
