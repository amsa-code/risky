package au.gov.amsa.animator;

import static au.gov.amsa.geo.distance.EffectiveSpeedChecker.effectiveSpeedOk;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.geo.distance.EffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;

public class ModelManyCraft implements Model {

    private final FixesSubscriber subscriber;
    private volatile long stepNumber;

    public ModelManyCraft() {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        this.subscriber = new FixesSubscriber();
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file)).take(10000000)
                .cache().onBackpressureBuffer().doOnCompleted(() -> subscriber.reset()).repeat()
                .doOnCompleted(() -> {
                    System.out.println("completed");
                });
        //
        // .doOnNext(System.out::println);
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long stepNumber) {
        this.stepNumber = stepNumber;
        subscriber.requestMore(10000);
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
            if (last == null
                    || f.time() >= last.time() + 600000
                    && effectiveSpeedOk(last.time(), last.lat(), last.lon(), f.time(), f.lat(),
                            f.lon(), options)) {
                queue.add(f);
                lastFix.put(f.mmsi(), f);
            }
        }
    }

    private static Func1<List<Fix>, Fix> extrapolateToNext(long startTime, long intervalMs) {
        return list -> {
            if (list.size() == 0)
                throw new RuntimeException("unexpected");
            else if (list.size() == 1) {
                return list.get(0);
            } else {
                Fix a = list.get(0);
                Fix b = list.get(1);
                long t = ((b.time() - startTime) / intervalMs + 1) * intervalMs + startTime;
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
        };
    }

    public static void main(String[] args) {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file));
        long startTime = source.toBlocking().first().time();

        final long intervalMs = TimeUnit.MINUTES.toMillis(5);

        source.buffer(100000)
                .take(1)
                //
                .concatMap(
                        buffer -> Observable.from(buffer)
                        // sort by time
                                .toSortedList((a, b) -> (((Long) a.time()).compareTo(b.time())))
                                // flatten
                                .concatMap(x -> Observable.from(x))
                                // group by timestep
                                .groupBy(fix -> (fix.time() - startTime) / intervalMs)
                                //
                                .flatMap(
                                // group by mmsi
                                        g -> g.groupBy(fix -> fix.mmsi())
                                        //
                                                .flatMap(
                                                        g2 -> // take the last
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
                                                                .map(extrapolateToNext(startTime,
                                                                        intervalMs)))))
                .subscribe(System.out::println);
    }

}
