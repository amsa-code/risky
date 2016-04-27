package au.gov.amsa.ais.router.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

import au.gov.amsa.streams.StringServer;
import rx.Observable;

public final class Port implements Closeable {

    private final int port;
    private final Optional<Group> group;
    private final boolean enabled;
    private final Optional<UserGroup> userGroup;
    private final Observable<String> lines;

    private Port(int port, Optional<Group> group, boolean enabled, Optional<UserGroup> userGroup) {
        this.port = port;
        this.group = group;
        this.enabled = enabled;
        this.userGroup = userGroup;
        this.lines = createLines();
    }

    private Observable<String> createLines() {
        if (group.isPresent()) {
            return group.get().lines();
        } else {
            return Observable.empty();
        }
    }

    public int port() {
        return port;
    }

    public Optional<Group> group() {
        return group;
    }

    public Optional<UserGroup> userGroup() {
        return userGroup;
    }

    public boolean enabled() {
        return enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    private Optional<StringServer> server = Optional.empty();
    private final Object serverLock = new Object();

    public void start() {
        synchronized (serverLock) {
            if (server.isPresent()) {
                throw new RuntimeException("server already started");
            } else if (enabled && group.isPresent()) {
                server = Optional.of(StringServer.create(lines, port));
                server.get().start();
            }
        }
    }

    public void stop() {
        synchronized (serverLock) {
            if (server.isPresent()) {
                server.get().stop();
            }
        }
    }

    public static class Builder {

        private int port;
        private Optional<Group> group = Optional.empty();
        private final Optional<UserGroup> userGroup = Optional.empty();
        private boolean enabled = true;

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
            return new Port(port, group, enabled, userGroup);
        }
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}
