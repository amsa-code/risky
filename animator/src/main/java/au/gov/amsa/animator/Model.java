package au.gov.amsa.animator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;

import com.google.common.base.Optional;

public class Model {

    volatile long timeStep = 0;
    private final FixesSubscriber subscriber;

    public Model() {
        File file = new File("/media/an/binary-fixes-5-minute/2014/503433000.track");
        Observable<Fix> source = BinaryFixes.from(file, true);
        subscriber = new FixesSubscriber();
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
        subscriber.requestMore(1);
    }

    public Optional<Fix> latest() {
        return Optional.fromNullable(subscriber.latest);
    }

    public List<Fix> recent() {
        return new ArrayList<Fix>(subscriber.queue);
    }

    private static class FixesSubscriber extends Subscriber<Fix> {

        volatile Fix latest;
        private ConcurrentLinkedQueue<Fix> queue = new ConcurrentLinkedQueue<Fix>();
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

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Fix t) {
            if (queue.size() == maxSize)
                queue.poll();
            queue.add(t);
            latest = t;
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        File file = new File("/media/an/binary-fixes-5-minute/2014/565187000.track");
        Observable<Fix> source = BinaryFixes.from(file, true);
        source.subscribe(System.out::println);
    }
}
