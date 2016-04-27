package au.gov.amsa.ais.router.model;

import java.util.Optional;

import com.github.davidmoten.util.Preconditions;

public final class Proxy {
    private final String host;
    private final int port;
    private final Optional<Authentication> authentication;

    private Proxy(String host, int port, Optional<Authentication> authentication) {
        Util.verifyNotBlank("host", host);
        Preconditions.checkNotNull(authentication);
        this.host = host;
        this.port = port;
        this.authentication = authentication;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public Optional<Authentication> getAuthentication() {
        return authentication;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String host;
        private int port;
        private Optional<Authentication> authentication;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder authentication(Optional<Authentication> authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder authentication(Authentication authentication) {
            return authentication(Optional.of(authentication));
        }

        public Proxy build() {
            return new Proxy(host, port, authentication);
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Proxy [host=");
        b.append(host);
        b.append(", port=");
        b.append(port);
        b.append(", authentication=");
        b.append(authentication);
        b.append("]");
        return b.toString();
    }

}
