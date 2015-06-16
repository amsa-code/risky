package au.gov.amsa.animator;

import java.io.File;

import rx.Observable;
import rx.Subscriber;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;

import com.google.common.base.Optional;

public class Model {

    volatile long timeStep = 0;
    private final FixesSubscriber subscriber;

    public Model() {
        // 565187000
        File file = new File("/home/dxm/565187000.track.gz");
        Observable<Fix> source = BinaryFixes.from(file, true);
        subscriber = new FixesSubscriber();
        // source.subscribeOn(Schedulers.io()).subscribe();
    }

    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
        subscriber.requestMore(1);
    }

    public Optional<Fix> latest() {
        return Optional.fromNullable(subscriber.latest);
    }

    private static class FixesSubscriber extends Subscriber<Fix> {

        volatile Fix latest;

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
            latest = t;
        }

    }
}
