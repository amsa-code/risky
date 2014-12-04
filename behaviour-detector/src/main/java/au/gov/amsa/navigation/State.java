package au.gov.amsa.navigation;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.google.common.base.Optional;

import fj.Equal;
import fj.F;
import fj.Ord;
import fj.Ordering;
import fj.P3;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;

class State {

	private static Logger log = LoggerFactory.getLogger(State.class);

	private static final int R_TREE_MAX_CHILDREN = 10;
	private final Map<Identifier, Set<VesselPosition>> map;
	private final RTree<VesselPosition,Point> tree;
	private final Optional<VesselPosition> last;
	private final long counter;
	private Set<VesselPosition> byTimeAndId;

	private State(Map<Identifier, Set<VesselPosition>> map,
			Set<VesselPosition> byTimeAndId, RTree<VesselPosition,Point> tree,
			Optional<VesselPosition> last, long counter) {
		this.map = Collections.unmodifiableMap(map);
		this.byTimeAndId = byTimeAndId;
		this.tree = tree;
		this.last = last;
		this.counter = counter;
	}

	State() {
		this(new HashMap<Identifier, Set<VesselPosition>>(), EMPTY, RTree
				.star().maxChildren(R_TREE_MAX_CHILDREN)
				.<VesselPosition,Point> create(), Optional.<VesselPosition> absent(),
				0);
	}

	private static Ord<VesselPosition> ordering = toOrdering(Comparators.timeIdMessageIdComparator);
	private static Set<VesselPosition> EMPTY = Set.empty(ordering);

	

	Optional<VesselPosition> nextPosition() {
		if (last.isPresent())
			return next(last.get());
		else
			return Optional.absent();
	}

	private Optional<VesselPosition> next(VesselPosition p) {
		Iterator<VesselPosition> it = map.get(p.id()).split(p)._3().iterator();
		return it.hasNext() ? Optional.of(it.next()) : Optional
				.<VesselPosition> absent();
	}

	RTree<VesselPosition,Point> tree() {
		return tree;
	}

	Optional<VesselPosition> last() {
		return last;
	}

	private static Set<VesselPosition> add(Set<VesselPosition> set,
			VesselPosition t) {
		return set.insert(t);
	}

	State nextState(final long maxTimeInterval, VesselPosition p) {

		// add p to byTime, tree and map
		Set<VesselPosition> newByTime = add(byTimeAndId, p);
		RTree<VesselPosition,Point> newTree = tree.add(p, geometry(p));
		Map<Identifier, Set<VesselPosition>> newMap = new HashMap<>(map);
		addToMap(newMap, p);

		// remove expired vessel positions
		if (counter % 10000 == 0) {
			// remove items from the map
			long t = System.currentTimeMillis();
			P3<Set<VesselPosition>, Option<VesselPosition>, Set<VesselPosition>> split = newByTime
					.split(VesselPosition.builder()
							.time(p.time() - maxTimeInterval).id(new Mmsi(0))
							.build());
			Set<VesselPosition> removeThese = split._1();
			// remove items from the byTime set
			newByTime = split._3();

			List<List<VesselPosition>> lists = removeThese.toList().group(
					EQUAL_ID);

			int count = 0;
				for (List<VesselPosition> list:lists) {
					Identifier id = list.index(0).id();
					Set<VesselPosition> removals = Set.iterableSet(ordering, list);
					Set<VesselPosition> set = newMap.get(id);
					set = set.minus(removals);
					if (set.size() == 0)
						newMap.remove(id);
					else
						newMap.put(id, set);
					count+=list.length();
			}
//			log.info("removed " + count + " from map");

			// remove items from the tree
//			log.info("removing from tree");
			for (VesselPosition vp : removeThese) {
				newTree = newTree.delete(vp, geometry(vp));
			}
//			log.info("removed from tree");
			if (newTree.size() != newByTime.size())
				throw new RuntimeException("unexpected");
			t = System.currentTimeMillis() - t;
			log.info("removed " + count + " in " + t + "ms");
		}
		return new State(newMap, newByTime, newTree, Optional.of(p),
				counter + 1);
	}

	private static void addToMap(Map<Identifier, Set<VesselPosition>> map,
			VesselPosition p) {
		Optional<Set<VesselPosition>> existing = Optional.fromNullable(map
				.get(p.id()));
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
			public F<A, Ordering> f(final A a1) {
				return new F<A, Ordering>() {
					public Ordering f(final A a2) {
						final int x = comparator.compare(a1, a2);
						return x < 0 ? Ordering.LT : x == 0 ? Ordering.EQ
								: Ordering.GT;
					}
				};
			}
		});
	}
	
}
