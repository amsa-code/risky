package au.gov.amsa.ais.router.model;

public final class Port {
    private final int port;
    private final Group group;
    private final boolean enabled;

    private Port(int port, Group group, boolean enabled) {
        this.port = port;
        this.group = group;
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public Group getGroup() {
        return group;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int port;
        private Group group;
        private boolean enabled;

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder group(Group group) {
            this.group = group;
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
