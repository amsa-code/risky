package au.gov.amsa.navigation;

import static com.github.davidmoten.rtree.geometry.Geometries.rectangle;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;
import static rx.Observable.empty;
import static rx.Observable.from;
import static rx.Observable.just;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;

public class CollisionDetector {

    private static final long MAX_TIME_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    private static final double MAX_VESSEL_SPEED_METRES_PER_SECOND = 24;

    private static final double LATITUDE_DELTA = 2 * MAX_TIME_INTERVAL_MS / 1000
            * MAX_VESSEL_SPEED_METRES_PER_SECOND / (60 * 1852);

    // TODO use this?
    // private static final long STEP_MS = TimeUnit.SECONDS.toMillis(1);

    public Observable<CollisionCandidate> getCandidates(Observable<VesselPosition> o) {

        return getCandidatesForAStream(o);

        // TODOs
        // split the stream into multiple streams based on slightly overlapping
        // geographic region (overlap is LATITUDE_DELTA and longitudeDelta(lat)
        // in size) to enable concurrency
        // .groupBy(toRegion()).flatMap(getCandidates());

    }

    public static Transformer<VesselPosition, CollisionCandidate> detectCollisionCandidates() {
        return o -> new CollisionDetector().getCandidates(o);
    }

    private static Func1<VesselPosition, Region> toRegion() {
        return new Func1<VesselPosition, Region>() {

            @Override
            public Region call(VesselPosition p) {
                double maxLat = 15;
                double minLat = -50;
                double minLon = -70;
                double maxLon = 179;
                int numRegions = Runtime.getRuntime().availableProcessors();
                int x = (int) Math.floor((p.lon() - minLon) / (maxLon - minLon) * numRegions);
                double lonCellSize = (maxLon - minLon) / numRegions;
                double longitudeDelta;
                if (Math.abs(minLat) > Math.abs(maxLat))
                    longitudeDelta = longitudeDelta(minLat);
                else
                    longitudeDelta = longitudeDelta(maxLat);
                return new Region(maxLat, minLon + x * lonCellSize - longitudeDelta, minLat, minLon
                        + (x + 1) * lonCellSize + longitudeDelta);
            }
        };
    }

    public static Observable<CollisionCandidate> getCandidatesForAStream(
            Observable<VesselPosition> o) {
        // make a window of recent positions indexed spatially
        return o.scan(new State(), nextState())
                // log
                .lift(Logging
                        .<State> logger()
                        .showCount("positions")
                        .showRateSince("rate (pos/s)", TimeUnit.SECONDS.toMillis(10))
                        .showRateSinceStart("overall rate")
                        .every(10000)
                        .showValue()
                        .value(state -> "state.map.size=" + state.mapSize() + ", state.rtree.size="
                                + state.tree().size()).log())
                // report collision candidates from each window for the latest
                // reported position
                .flatMap(toCollisionCandidatesForPosition())
                // group by id of first candidate
                .groupBy(byIdPair())
                // only show if repeated
                .flatMap(onlyRepeating());
    }

    private static Func2<State, VesselPosition, State> nextState() {
        return (state, p) -> state.nextState(MAX_TIME_INTERVAL_MS, p);
    }

    private static Func1<State, Observable<CollisionCandidate>> toCollisionCandidatesForPosition() {
        return state -> {
            if (!state.last().isPresent())
                return Observable.empty();
            else {
                return toCollisionCandidatesForPosition(state);
            }
        };
    }

    private static Observable<CollisionCandidate> toCollisionCandidatesForPosition(State state) {
        final VesselPosition p = state.last().get();
        final Optional<VesselPosition> next = state.nextPosition();

        // use the spatial index to get positions physically near the latest
        // position report

        // setup a region around the latest position report to search with a
        // decent delta).
        double longitudeDelta = longitudeDelta(p.lat());
        Rectangle searchRegion = rectangle(p.lon() - longitudeDelta, p.lat() - LATITUDE_DELTA,
                p.lon() + longitudeDelta, p.lat() + LATITUDE_DELTA);

        // find nearby vessels within time constraints and cache them
        Observable<VesselPosition> near = state.tree()
        // search the R-tree
                .search(searchRegion)
                // get just the vessel position
                .map(toVesselPosition)
                // only accept positions with time close to p
                .filter(aroundInTime(p, MAX_TIME_INTERVAL_MS));

        final Observable<TreeSet<VesselPosition>> othersByVessel = near
        // only those vessels with different id as latest position report
                .filter(not(isVessel(p.id())))
                // group by individual vessel
                .groupBy(byId())
                // sort the positions by time
                .flatMap(toSortedSet());

        Observable<CollisionCandidate> collisionCandidates = othersByVessel
                .flatMap(toCollisionCandidates2(p, next));

        return collisionCandidates;
    }

    private static <T> Func1<T, Boolean> not(final Func1<T, Boolean> f) {
        return t -> !f.call(t);
    }

    private static double longitudeDelta(double lat) {
        return LATITUDE_DELTA / cos(toRadians(lat));
    }

    private static Func1<GroupedObservable<IdentifierPair, CollisionCandidate>, Observable<? extends CollisionCandidate>> onlyRepeating() {
        return g -> g.buffer(2).flatMap(isSmallTimePeriod());
    }

    private static Func1<List<CollisionCandidate>, Observable<CollisionCandidate>> isSmallTimePeriod() {
        return list -> {
            Optional<Long> min = Optional.absent();
            Optional<Long> max = Optional.absent();
            for (CollisionCandidate c : list) {
                if (!min.isPresent() || c.position1().time() < min.get())
                    min = Optional.of(c.position1().time());
                if (!max.isPresent() || c.position1().time() > max.get())
                    max = Optional.of(c.position1().time());
            }
            if (max.get() - min.get() < TimeUnit.MINUTES.toMillis(5))
                return from(list);
            else
                return empty();
        };
    }

    private static Func1<? super CollisionCandidate, IdentifierPair> byIdPair() {
        return c -> new IdentifierPair(c.position1().id(), c.position2().id());
    }

    private static Func1<TreeSet<VesselPosition>, Observable<CollisionCandidate>> toCollisionCandidates2(
            final VesselPosition p, final Optional<VesselPosition> next) {
        return set -> {
            Optional<VesselPosition> other = Optional.fromNullable(set.lower(p));
            if (other.isPresent()) {
                Optional<Times> times = p.intersectionTimes(other.get());
                if (times.isPresent()) {
                    Optional<Long> tCollision = plus(times.get().leastPositive(), p.time());
                    if (tCollision.isPresent()
                            && tCollision.get() < p.time() + MAX_TIME_INTERVAL_MS) {
                        Optional<VesselPosition> otherNext = Optional.fromNullable(set.higher(other
                                .get()));
                        if (otherNext.isPresent() && otherNext.get().time() < tCollision.get())
                            return empty();
                        else if (next.isPresent() && next.get().time() < tCollision.get())
                            return empty();
                        else
                            return just(new CollisionCandidate(p, other.get(), tCollision.get()));
                    } else
                        return empty();
                } else
                    return empty();
            } else
                return empty();
        };
    }

    private static Optional<Long> plus(Optional<Long> a, long b) {
        if (a.isPresent())
            return Optional.of(a.get() + b);
        else
            return Optional.absent();
    }

    private static Func1<GroupedObservable<Identifier, VesselPosition>, Observable<TreeSet<VesselPosition>>> toSortedSet() {
        return g -> g.toList().map(
                singleVesselPositions -> {
                    TreeSet<VesselPosition> set = new TreeSet<VesselPosition>(
                            Comparators.timeIdMessageIdComparator);
                    set.addAll(singleVesselPositions);
                    return set;
                });
    }

    private static Func1<VesselPosition, Identifier> byId() {
        return position -> position.id();
    }

    private static Func1<VesselPosition, Boolean> aroundInTime(final VesselPosition position,
            final long maxTimeIntervalMs) {
        return p -> Math.abs(p.time() - position.time()) <= maxTimeIntervalMs;
    }

    private static Func1<VesselPosition, Boolean> isVessel(final Identifier id) {
        return p -> p.id().equals(id);
    }

    private static Func1<Entry<VesselPosition, Point>, VesselPosition> toVesselPosition = entry -> entry
            .value();

}
