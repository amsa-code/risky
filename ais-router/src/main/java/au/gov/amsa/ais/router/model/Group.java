package au.gov.amsa.ais.router.model;

import java.util.List;
import java.util.regex.Pattern;

import com.github.davidmoten.guavamini.Preconditions;

import rx.Observable;
import rx.schedulers.Schedulers;

public final class Group implements GroupMember {

    private final String id;
    private final List<GroupMember> members;
    private final boolean enabled;
    private final boolean addTimestamp;
    private final boolean addArrivalTime;
    private final List<Region> filterRegions;
    private final List<MessageType> filterMessageTypes;
    private final List<Pattern> filterPatterns;

    private Group(String id, List<GroupMember> members, boolean enabled, boolean addTimestamp,
            boolean addArrivalTime, List<Region> filterRegions,
            List<MessageType> filterMessageTypes, List<Pattern> filterPatterns) {
        Util.verifyId(id);
        Preconditions.checkNotNull(members);
        Preconditions.checkNotNull(filterRegions);
        this.id = id;
        this.members = members;
        this.enabled = enabled;
        this.addTimestamp = addTimestamp;
        this.addArrivalTime = addArrivalTime;
        this.filterRegions = filterRegions;
        this.filterMessageTypes = filterMessageTypes;
        this.filterPatterns = filterPatterns;
    }

    @Override
    public Observable<String> lines() {
        if (enabled) {
            // TODO filter on regions and message types
            return Observable.from(members)
                    .flatMap(member -> member.lines().subscribeOn(Schedulers.computation()));
        } else {
            return Observable.empty();
        }
    }

    public String id() {
        return id;
    }

    public List<GroupMember> members() {
        return members;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean addTimestamp() {
        return addTimestamp;
    }

    public boolean addArrivalTime() {
        return addArrivalTime;
    }

    public List<Region> filterRegions() {
        return filterRegions;
    }

    public List<MessageType> filterMessageTypes() {
        return filterMessageTypes;
    }

    public List<Pattern> filterPatterns() {
        return filterPatterns;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private List<GroupMember> members;
        private boolean enabled;
        private boolean addTimestamp;
        private boolean addArrivalTime;
        private List<Region> filterRegions;
        private List<MessageType> filterMessageTypes;
        private List<Pattern> filterPatterns;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder members(List<GroupMember> members) {
            this.members = members;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder addTimestamp(boolean addTimestamp) {
            this.addTimestamp = addTimestamp;
            return this;
        }

        public Builder addArrivalTime(boolean addArrivalTime) {
            this.addArrivalTime = addArrivalTime;
            return this;
        }

        public Builder filterRegions(List<Region> filterRegions) {
            this.filterRegions = filterRegions;
            return this;
        }

        public Builder filterMessageTypes(List<MessageType> filterMessageTypes) {
            this.filterMessageTypes = filterMessageTypes;
            return this;
        }

        public Builder filterPattern(List<Pattern> filterPatterns) {
            this.filterPatterns = filterPatterns;
            return this;
        }

        public Group build() {
            return new Group(id, members, enabled, addTimestamp, addArrivalTime, filterRegions,
                    filterMessageTypes, filterPatterns);
        }
    }

}
