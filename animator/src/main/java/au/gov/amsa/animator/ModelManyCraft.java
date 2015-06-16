package au.gov.amsa.animator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.Fix;

public class ModelManyCraft implements Model {

    private final FixesSubscriber subscriber;
    private volatile long timeStep;

    public ModelManyCraft() {
        File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        Observable<Fix> source = Streams.extractFixes(Streams.nmeaFromGzip(file));
        this.subscriber = new FixesSubscriber();
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
    }

    @Override
    public Map<Long, List<Fix>> recent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long stepNumber() {
        // TODO Auto-generated method stub
        return 0;
    }

    private static class FixesSubscriber extends Subscriber<Fix> {

        volatile Fix latest;
        private final ConcurrentLinkedQueue<Fix> queue = new ConcurrentLinkedQueue<Fix>();
        private final int maxSize = 100;

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
        public void onNext(Fix t) {
            if (queue.size() == maxSize)
                queue.poll();
            queue.add(t);
            latest = t;
        }

    }

}
