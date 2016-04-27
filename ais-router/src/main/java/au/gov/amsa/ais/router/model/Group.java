package au.gov.amsa.ais.router.model;

import java.util.ArrayList;
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
    private final Observable<String> lines;

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
        this.lines = createLines();
    }

    @Override
    public Observable<String> lines() {
        return lines;
    }

    private Observable<String> createLines() {
        if (enabled) {
            return Observable.from(members)
                    // concurrently merge member observables
                    .flatMap(member -> member.lines().subscribeOn(Schedulers.io()))
                    // TODO filter on regions and message types
                    // filter on patterns
                    .filter(line -> filterPatterns.stream()
                            .anyMatch(pattern -> pattern.matcher(line).matches()))
                    // multiple parent groups share the same stream
                    .share();
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
        private final List<GroupMember> members = new ArrayList<GroupMember>();
        private boolean enabled = true;
        private boolean addTimestamp;
        private boolean addArrivalTime;
        private final List<Region> filterRegions = new ArrayList<>();
        private final List<MessageType> filterMessageTypes = new ArrayList<MessageType>();
        private final List<Pattern> filterPatterns = new ArrayList<Pattern>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder members(List<GroupMember> members) {
            this.members.addAll(members);
            return this;
        }

        public Builder member(GroupMember member) {
            this.members.add(member);
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
            this.filterRegions.addAll(filterRegions);
            return this;
        }

        public Builder filterRegion(Region filterRegion) {
            this.filterRegions.add(filterRegion);
            return this;
        }

        public Builder filterMessageTypes(List<MessageType> filterMessageTypes) {
            this.filterMessageTypes.addAll(filterMessageTypes);
            return this;
        }

        public Builder filterMessageTypes(MessageType filterMessageType) {
            this.filterMessageTypes.add(filterMessageType);
            return this;
        }

        public Builder filterPatterns(List<Pattern> filterPatterns) {
            this.filterPatterns.addAll(filterPatterns);
            return this;
        }

        public Builder filterPattern(Pattern filterPattern) {
            this.filterPatterns.add(filterPattern);
            return this;
        }

        public Group build() {
            return new Group(id, members, enabled, addTimestamp, addArrivalTime, filterRegions,
                    filterMessageTypes, filterPatterns);
        }
    }

}
