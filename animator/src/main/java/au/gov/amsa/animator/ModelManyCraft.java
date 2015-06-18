package au.gov.amsa.animator;

import static au.gov.amsa.geo.distance.EffectiveSpeedChecker.effectiveSpeedOk;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.Fix;

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

    public static void main(String[] args) {
        Observable.just(1, 2, 3).delay(1, TimeUnit.SECONDS, Schedulers.immediate()).cache()
                .repeat(2).doOnNext(System.out::println).subscribe();
        // TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        // Observable.just(1, 2,
        // 3).doOnRequest(System.out::println).repeat().subscribe(ts);
        // ts.requestMore(10);
        // ts.assertValueCount(10);
        // File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        // Streams.extractFixes(Streams.nmeaFromGzip(file))
        // .filter(fix -> fix.lat() < -25 && fix.lat() > -35 && fix.lon() > 110
        // && fix.lon() < 115).forEach(System.out::println);
    }
}
