package au.gov.amsa.ais.router.model;

import java.util.Optional;

public final class Port {
    private final int port;
    private final Optional<Group> group;
    private final boolean enabled;

    private Port(int port, Optional<Group> group, boolean enabled) {
        this.port = port;
        this.group = group;
        this.enabled = enabled;
    }

    public int port() {
        return port;
    }

    public Optional<Group> group() {
        return group;
    }

    public boolean enabled() {
        return enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int port;
        private Optional<Group> group = Optional.empty();
        private boolean enabled;

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder group(Group group) {
            this.group = Optional.of(group);
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Port build() {
            return new Port(port, group, enabled);
        }
    }
}
