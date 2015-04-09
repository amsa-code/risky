package au.gov.amsa.navigation;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.DriftDetector.DriftDetectorTransformer;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.Fixes;
import au.gov.amsa.streams.StringSockets;

import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DriftDetectorFromAisStreamMain {

    private static final Logger log = Logger.getLogger(DriftDetectorFromAisStreamMain.class);

    public static void main(String[] args) {
        Observable<String> lines = StringSockets
        //
                .from("sarappsdev.amsa.gov.au")
                //
                .port(9010)
                //
                .quietTimeout(1, TimeUnit.MINUTES)
                //
                .reconnectDelay(1, TimeUnit.SECONDS)
                //
                .create();

        final Map<Long, DriftDetectorTransformer> detectors = Maps.asMap(Sets.<Long> newHashSet(),
                new Function<Long, DriftDetectorTransformer>() {

                    @Override
                    public DriftDetectorTransformer apply(Long input) {
                        return DriftDetector.detectDrift();
                    }
                });

        Streams.extractFixes(lines)
        //
                .lift(Logging.<Fix> logger().showCount().every(100).log())
                //
                .groupBy(new Func1<Fix, Long>() {
                    @Override
                    public Long call(Fix fix) {
                        return fix.mmsi();
                    }
                }).flatMap(new Func1<GroupedObservable<Long, Fix>, Observable<DriftCandidate>>() {

                    @Override
                    public Observable<DriftCandidate> call(GroupedObservable<Long, Fix> g) {
                        return g.compose(Fixes.ignoreOutOfOrderFixes(true))
                        // detect drift
                                .compose(detectors.get(g.getKey()))
                                // downsample to min 5 minutes between
                                // reports but ensure that start of
                                // drift is always included
                                .compose(
                                        Downsample.<DriftCandidate> minTimeStep(5,
                                                TimeUnit.MINUTES, isStartOfDrift()));
                    }
                })

                // retry on error
                .retry()
                // go!
                .subscribe(new Observer<Object>() {

                    @Override
                    public void onCompleted() {
                        log.info("finished");
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.error(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Object t) {
                        log.info(t);
                    }
                });
    }

    private static Func1<DriftCandidate, Boolean> isStartOfDrift() {
        return new Func1<DriftCandidate, Boolean>() {
            @Override
            public Boolean call(DriftCandidate c) {
                return c.driftingSince() == c.fix().time();
            }
        };
    }
}
