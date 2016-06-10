package au.gov.amsa.streams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.Checked;
import com.github.davidmoten.rx.RetryWhen;
import com.github.davidmoten.util.Preconditions;
import com.google.common.annotations.VisibleForTesting;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public final class StringSockets {

    private static final Logger log = LoggerFactory.getLogger(StringSockets.class);

    /**
     * Returns an Observable sequence of strings (not lines) from the given host
     * and port. If the stream is quiet for <code>quietTimeoutMs</code> then a
     * reconnect will be attempted. Note that this is a good idea with TCPIP
     * connections as for instance a firewall can simply drop a quiet connection
     * without the client being aware of it. If any exception occurs a reconnect
     * will be attempted after <code>reconnectDelayMs</code>. If the socket is
     * closed by the server (the end of the input stream is reached) then a
     * reconnect is attempted after <code>reconnectDelayMs</code>.
     * 
     * @param hostPort
     * @return Observable sequence of strings (not lines).
     */
    public static Observable<String> from(final String host, final int port, long quietTimeoutMs,
            long reconnectDelayMs, Charset charset, Scheduler scheduler) {
        // delay connect by delayMs so that if server closes
        // stream on every connect we won't be in a mad loop of
        // failing connections
        return strings(host, port, (int) quietTimeoutMs, charset) //
                // additional timeout appears to be necessary for certain use
                // cases like when the server side does not close the socket
                .timeout(quietTimeoutMs + 100, TimeUnit.MILLISECONDS) //
                .subscribeOn(scheduler) //
                // if any exception occurs retry
                .retryWhen(RetryWhen //
                        .delay(reconnectDelayMs, TimeUnit.MILLISECONDS) //
                        .build()) //
                // all subscribers use the same stream
                .share();
    }

    public static class Builder {
        private final String host;
        private int port = 6564;
        private long quietTimeoutMs = 60000;
        private long reconnectDelayMs = 30000;
        private Charset charset = StandardCharsets.UTF_8;
        private Scheduler scheduler = Schedulers.io();

        Builder(String host) {
            this.host = host;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder quietTimeoutMs(long quietTimeoutMs) {
            this.quietTimeoutMs = quietTimeoutMs;
            return this;
        }

        public Builder quietTimeout(long duration, TimeUnit unit) {
            return quietTimeoutMs(unit.toMillis(duration));
        }

        public Builder reconnectDelayMs(long reconnectDelayMs) {
            this.reconnectDelayMs = reconnectDelayMs;
            return this;
        }

        public Builder reconnectDelay(long duration, TimeUnit unit) {
            return reconnectDelayMs(unit.toMillis(duration));
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder subscribeOn(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Observable<String> create() {
            return from(host, port, quietTimeoutMs, reconnectDelayMs, charset, scheduler);
        }

    }

    /**
     * Returns a builder for converting a socket read to an Observable. Defaults
     * to port=6564, quietTimeoutMs=60000, reconnectDelayMs=30000, charset=
     * UTF-8.
     * 
     * @param host
     * @return
     */
    public static Builder from(String host) {
        return new Builder(host);
    }

    public static Observable<String> strings(final String host, final int port, int quietTimeoutMs,
            final Charset charset) {
        Preconditions.checkNotNull(host);
        Preconditions.checkArgument(port >= 0 && port <= 65535, "port must be between 0 and 65535");
        Preconditions.checkArgument(quietTimeoutMs > 0, "quietTimeoutMs must be > 0");
        Preconditions.checkNotNull(charset);
        return Observable
                // create a stream from a socket and dispose of socket
                // appropriately
                .using(socketCreator(host, port, quietTimeoutMs), socketObservableFactory(charset),
                        socketDisposer(), true);
    }

    @VisibleForTesting
    static Func0<Socket> socketCreator(final String host, final int port, long quietTimeoutMs) {
        return Checked.f0(() -> {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout((int) quietTimeoutMs);
            return socket;
        });
    }

    @VisibleForTesting
    static Func1<Socket, Observable<String>> socketObservableFactory(final Charset charset) {
        return Checked.f1(
                socket -> Strings.from(new InputStreamReader(socket.getInputStream(), charset)));
    }

    @VisibleForTesting
    static Action1<Socket> socketDisposer() {
        return socket -> {
            try {
                log.info("closing socket " + socket.getInetAddress().getHostAddress() + ":"
                        + socket.getPort());
                socket.close();
            } catch (IOException e) {
                // don't really care if socket could not be closed cleanly
                log.info("messageOnSocketClose=" + e.getMessage(), e);
            }
        };
    }

    public static Observable<String> mergeLinesFrom(Observable<HostPort> hostPorts,
            Scheduler scheduler) {
        return hostPorts
                //
                .map(hp -> StringSockets
                        .from(hp.getHost(), hp.getPort(), hp.getQuietTimeoutMs(),
                                hp.getReconnectDelayMs(), StandardCharsets.UTF_8, scheduler)
                        // split by new line character
                        .compose(o -> Strings.split(o, "\n")))
                // merge streams of lines
                .compose(o -> Observable.merge(o));
    }

    public static Observable<String> mergeLinesFrom(Observable<HostPort> hostPorts) {
        return mergeLinesFrom(hostPorts, Schedulers.io());
    }

    public static void main(String[] args) {
        Observable<HostPort> hostPorts = Observable.just(
                HostPort.create("sarapps", 9010, 1000, 1000),
                HostPort.create("sarapps", 9100, 1000, 1000));
        StringSockets.mergeLinesFrom(hostPorts, Schedulers.io()).doOnNext(System.out::println)
                .toBlocking().last();
    }
}
