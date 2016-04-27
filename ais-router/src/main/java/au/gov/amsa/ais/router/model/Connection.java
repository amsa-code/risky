package au.gov.amsa.ais.router.model;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.rx.Transformers;

import au.gov.amsa.streams.StringSockets;
import rx.Observable;

public final class Connection implements GroupMember {

    private final String id;
    private final String host;
    private final int port;
    private final boolean ssl;
    private final Optional<Authentication> authentication;
    private final int readTimeoutMs;
    private final long retryIntervalMs;
    private final boolean enabled;
    private final Observable<String> lines;

    private Connection(String id, String host, int port, boolean ssl,
            Optional<Authentication> authentication, int readTimeoutMs, long retryIntervalMs,
            boolean enabled) {
        Util.verifyId(id);
        Preconditions.checkNotNull(host);
        Preconditions.checkArgument(port > 0 && port <= 65535, "port must be between 0 and 65535");
        Preconditions.checkNotNull(authentication);
        Preconditions.checkArgument(readTimeoutMs >= 0, "readTimeMs must be >=0");
        Preconditions.checkArgument(retryIntervalMs >= 0, "retryIntervalMs must be >=0");
        this.id = id;
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.authentication = authentication;
        this.readTimeoutMs = readTimeoutMs;
        this.retryIntervalMs = retryIntervalMs;
        this.enabled = enabled;
        // multiple parent groups share the same stream
        this.lines = StringSockets.from(host).charset(StandardCharsets.UTF_8).port(port)
                .quietTimeoutMs(readTimeoutMs).reconnectDelayMs(retryIntervalMs).create()
                .compose(Transformers.split("\n")).share();
    }

    public String id() {
        return id;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public boolean ssl() {
        return ssl;
    }

    public Optional<Authentication> authentication() {
        return authentication;
    }

    public int readTimeoutMs() {
        return readTimeoutMs;
    }

    public long retryIntervalMs() {
        return retryIntervalMs;
    }

    public boolean enabled() {
        return enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String host;
        private int port;
        private boolean ssl;
        private Optional<Authentication> authentication;
        private int readTimeoutMs;
        private long retryIntervalMs;
        private boolean enabled;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder authentication(Optional<Authentication> authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder readTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder retryIntervalMs(long retryIntervalMs) {
            this.retryIntervalMs = retryIntervalMs;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Connection build() {
            return new Connection(id, host, port, ssl, authentication, readTimeoutMs,
                    retryIntervalMs, enabled);
        }
    }

    @Override
    public Observable<String> lines() {
        return lines;
    }

}
