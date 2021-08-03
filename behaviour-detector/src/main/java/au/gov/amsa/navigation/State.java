package au.gov.amsa.navigation;


import static java.util.Optional.empty;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;
import fj.Equal;
import fj.F;
import fj.Ord;
import fj.Ordering;
import fj.P3;
import fj.data.Option;
import fj.data.Set;

class State {

    private static Logger log = LoggerFactory.getLogger(State.class);

    private static final int R_TREE_MAX_CHILDREN = 10;
    private final Map<Identifier, Set<VesselPosition>> map;
    private final RTree<VesselPosition, Point> tree;
    private final Optional<VesselPosition> last;
    private final long counter;
    private final Set<VesselPosition> byTimeAndId;

    private State(Map<Identifier, Set<VesselPosition>> map, Set<VesselPosition> byTimeAndId,
            RTree<VesselPosition, Point> tree, Optional<VesselPosition> last, long counter) {
        this.map = Collections.unmodifiableMap(map);
        this.byTimeAndId = byTimeAndId;
        this.tree = tree;
        this.last = last;
        this.counter = counter;
    }

    State() {
        this(new HashMap<Identifier, Set<VesselPosition>>(), EMPTY,
                RTree.star().maxChildren(R_TREE_MAX_CHILDREN).<VesselPosition, Point> create(),
                Optional.<VesselPosition> empty(), 0);
    }

    private static Ord<VesselPosition> ordering = toOrdering(Comparators.timeIdMessageIdComparator);
    private static Set<VesselPosition> EMPTY = Set.empty(ordering);

    Optional<VesselPosition> nextPosition() {
        if (last.isPresent())
            return next(last.get());
        else
            return Optional.empty();
    }

    private Optional<VesselPosition> next(VesselPosition p) {
        Iterator<VesselPosition> it = map.get(p.id()).split(p)._3().iterator();
        return it.hasNext() ? Optional.of(it.next()) : Optional.<VesselPosition> empty();
    }

    RTree<VesselPosition, Point> tree() {
        return tree;
    }

    Optional<VesselPosition> last() {
        return last;
    }

    private static Set<VesselPosition> add(Set<VesselPosition> set, VesselPosition t) {
        return set.insert(t);
    }

    private static final VesselPosition.Builder BUILDER = VesselPosition.builder()
            .positionAisNmea(empty()).cls(VesselClass.A).cogDegrees(Optional.of(0.0))
            .headingDegrees(empty()).data(empty()).lat(0.0).lon(0.0).lengthMetres(empty())
            .navigationalStatus(NavigationalStatus.NOT_DEFINED).shipStaticAisNmea(empty())
            .shipType(empty()).speedMetresPerSecond(Optional.of(0.0)).widthMetres(empty())
            .id(new Mmsi(0));

    State nextState(final long maxTimeInterval, VesselPosition p) {

        // add p to byTime, tree and map
        Set<VesselPosition> newByTime = add(byTimeAndId, p);
        RTree<VesselPosition, Point> newTree = tree.add(p, geometry(p));
        Map<Identifier, Set<VesselPosition>> newMap = new HashMap<>(map);
        addToMap(newMap, p);

        // remove expired vessel positions
        if (counter % 10000 == 0) {
            // remove items from the map
            long t = System.currentTimeMillis();
            P3<Set<VesselPosition>, Option<VesselPosition>, Set<VesselPosition>> split = newByTime
                    .split(BUILDER.time(p.time() - maxTimeInterval).build());
            Set<VesselPosition> removeThese = split._1();
            // remove items from the byTime set
            newByTime = split._3();

            ListMultimap<Identifier, VesselPosition> lists = ArrayListMultimap.create();
            for (VesselPosition vp : removeThese) {
                lists.put(vp.id(), vp);
            }

            int count = 0;
            for (Collection<VesselPosition> list : lists.asMap().values()) {
                Identifier id = list.iterator().next().id();
                Set<VesselPosition> removals = Set.iterableSet(ordering, list);
                Set<VesselPosition> set = newMap.get(id);
                set = set.minus(removals);
                if (set.size() == 0)
                    newMap.remove(id);
                else
                    newMap.put(id, set);
                count += list.size();
            }
            // log.info("removed " + count + " from map");

            // remove items from the tree
            // log.info("removing from tree");
            for (VesselPosition vp : removeThese) {
                newTree = newTree.delete(vp, geometry(vp));
            }
            // log.info("removed from tree");
            if (newTree.size() != newByTime.size())
                throw new RuntimeException("unexpected");
            t = System.currentTimeMillis() - t;
            log.info("removed " + count + " in " + t + "ms");
        }
        return new State(newMap, newByTime, newTree, Optional.of(p), counter + 1);
    }

    private static void addToMap(Map<Identifier, Set<VesselPosition>> map, VesselPosition p) {
        Optional<Set<VesselPosition>> existing = Optional.ofNullable(map.get(p.id()));
        if (existing.isPresent())
            map.put(p.id(), add(existing.get(), p));
        else
            map.put(p.id(), EMPTY.insert(p));
    }

    private static Point geometry(VesselPosition p) {
        return Geometries.point(p.lon(), p.lat());
    }

    public int mapSize() {
        return map.size();
    }

    private static final Equal<VesselPosition> EQUAL_ID = Equal
            .equal(new F<VesselPosition, F<VesselPosition, Boolean>>() {
                @Override
                public F<VesselPosition, Boolean> f(final VesselPosition a) {
                    return new F<VesselPosition, Boolean>() {

                        @Override
                        public Boolean f(VesselPosition b) {
                            return a.id().equals(b.id());
                        }
                    };
                }
            });

    private static <A> Ord<A> toOrdering(final Comparator<A> comparator) {
        return Ord.ord(new F<A, F<A, Ordering>>() {
            @Override
            public F<A, Ordering> f(final A a1) {
                return new F<A, Ordering>() {
                    @Override
                    public Ordering f(final A a2) {
                        final int x = comparator.compare(a1, a2);
                        return x < 0 ? Ordering.LT : x == 0 ? Ordering.EQ : Ordering.GT;
                    }
                };
            }
        });
    }

}
