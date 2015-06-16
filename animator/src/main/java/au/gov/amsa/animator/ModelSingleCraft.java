package au.gov.amsa.animator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;

public class ModelSingleCraft implements Model {

    volatile long timeStep = 0;
    private final FixesSubscriber subscriber;

    public ModelSingleCraft() {
        File file = new File("/media/an/binary-fixes-5-minute/2014/503433000.track");
        Observable<Fix> source = BinaryFixes.from(file, true);
        subscriber = new FixesSubscriber();
        source.subscribeOn(Schedulers.io()).subscribe(subscriber);
    }

    @Override
    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
        subscriber.requestMore(1);
    }

    @Override
    public Map<Long, Collection<Fix>> recent() {
        HashMap<Long, Collection<Fix>> map = new HashMap<Long, Collection<Fix>>();
        ArrayList<Fix> list = new ArrayList<>(subscriber.queue);
        if (list.size() > 0)
            map.put(list.get(0).mmsi(), list);
        return map;
    }

    private static class FixesSubscriber extends Subscriber<Fix> {

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

    @Override
    public long stepNumber() {
        return timeStep;
    }

}
