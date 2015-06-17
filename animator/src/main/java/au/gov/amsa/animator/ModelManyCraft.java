package au.gov.amsa.animator;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.Fixes;

public class ModelManyCraft implements Model {

    private final FixesSubscriber subscriber;
    private volatile long stepNumber;

    public ModelManyCraft() {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file))
                .filter(fix -> fix.mmsi() == 566674000).groupBy(fix -> fix.mmsi()).flatMap(g -> g
                //
                        .compose(Fixes.<Fix> ignoreOutOfOrderFixes(true))
                        //
                        //
                        // .doOnNext(System.out::println)
                        .compose(Downsample.minTimeStep(5, TimeUnit.MINUTES)))
                //
                .doOnNext(System.out::println);
        //
        // .compose(Downsample.minTimeStep(1, TimeUnit.MINUTES)));
        this.subscriber = new FixesSubscriber();
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long stepNumber) {
        this.stepNumber = stepNumber;
        subscriber.requestMore(1);
    }

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
        private final int maxSize = 50;

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
            queue.add(f);
        }
    }

    public static void main(String[] args) {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file))
                .groupBy(fix -> fix.mmsi())

                .flatMap(g -> g
                //
                        .compose(Fixes.<Fix> ignoreOutOfOrderFixes(true))
                        //
                        .compose(Downsample.minTimeStep(5, TimeUnit.MINUTES)))
                //
                .doOnNext(new Action1<Fix>() {
                    int n = 0;

                    @Override
                    public void call(Fix t) {
                        if (n++ % 1000 == 0) {
                            System.out.println(n);
                        }
                    }
                });
        source.subscribe();
    }
}
