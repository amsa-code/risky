package au.gov.amsa.ais.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.ais.router.model.Port;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class Router {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    public static Subscriber<Port> start(Port... ports) {
        Subscriber<Port> subscriber = createSubscriber();
        Observable //
                .from(ports) //
                .flatMap(port -> Observable //
                        .just(port) //
                        .doOnNext(p -> p.start()) //
                        .subscribeOn(Schedulers.io()))
                .subscribe(subscriber);
        return subscriber;
    }

    private static Subscriber<Port> createSubscriber() {
        return new Subscriber<Port>() {

            @Override
            public void onCompleted() {
                log.info("all ports stopped");
            }

            @Override
            public void onError(Throwable e) {
                log.error(e.getMessage(), e);
            }

            @Override
            public void onNext(Port t) {

            }
        };
    }
}
