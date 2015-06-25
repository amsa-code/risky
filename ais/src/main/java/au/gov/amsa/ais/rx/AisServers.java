package au.gov.amsa.ais.rx;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import au.gov.amsa.streams.HostPort;
import au.gov.amsa.streams.StringSockets;
import au.gov.amsa.streams.Strings;

public class AisServers {

    // used to inject a STOP sentinel message into the stream so that we can
    // stop the webapp at will
    private final PublishSubject<String> stopper = PublishSubject.create();

    private final List<HostPort> hostPorts;

    private static final String STOP_SENTINEL = "STOP_SENTINEL";

    public AisServers(List<HostPort> hostPorts) {
        this.hostPorts = hostPorts;
    }

    public Observable<String> nmea() {
        Observable<Observable<String>> ais = Observable.from(hostPorts)
        //
                .map(hp -> StringSockets
                        .from(hp.getHost(), hp.getPort(), hp.getQuietTimeoutMs(),
                                hp.getReconnectDelayMs(), StandardCharsets.UTF_8)
                        // split by new line character
                        .compose(o -> Strings.split(o, "\n"))
                        // make asynchronous
                        .subscribeOn(Schedulers.io()));

        return Observable.merge(stopper.serialize().nest().concatWith(ais))
        // check each line to see if we wish to stop
                .takeWhile(isNotStopSentinel());

    }

    private static Func1<String, Boolean> isNotStopSentinel() {
        return t -> !STOP_SENTINEL.equals(t);
    }

    public void stop() {
        stopper.onNext(STOP_SENTINEL);
    }

    public static void main(String[] args) {
        List<HostPort> hostPorts = Arrays.asList(HostPort.create("sarapps", 9010, 1000, 1000),
                HostPort.create("sarapps", 9100, 1000, 1000));
        new AisServers(hostPorts).nmea().toBlocking().last();
    }
}
