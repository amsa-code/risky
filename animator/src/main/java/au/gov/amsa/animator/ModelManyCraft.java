package au.gov.amsa.animator;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.Fix;

public class ModelManyCraft implements Model {

    private final FixesSubscriber subscriber;
    private volatile long stepNumber;

    public ModelManyCraft() {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file));
        //
        // .doOnNext(System.out::println);
        this.subscriber = new FixesSubscriber();
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long stepNumber) {
        this.stepNumber = stepNumber;
        subscriber.requestMore(1000);
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
        private final int maxSize = 1000;

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
        Streams.extractFixes(Streams.nmeaFromGzip(file))
                .filter(fix -> fix.lat() < -25 && fix.lat() > -35 && fix.lon() > 110
                        && fix.lon() < 115).forEach(System.out::println);
    }
}
