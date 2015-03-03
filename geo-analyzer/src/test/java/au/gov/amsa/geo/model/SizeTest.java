package au.gov.amsa.geo.model;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import rx.Observable;
import rx.internal.operators.OperatorSkip;

import com.google.common.util.concurrent.AtomicDouble;

public class SizeTest {

	// @Test
	public void test() throws InterruptedException {
		logMem();
		System.gc();
		Thread.sleep(3000);
		logMem();
		ConcurrentHashMap<Position, AtomicDouble> map = new ConcurrentHashMap<Position, AtomicDouble>();
		for (int i = 0; i < 1000000; i++) {
			Position p = new Position(i / Math.PI, i * Math.PI * Math.E);
			map.put(p, new AtomicDouble(Math.random() * 800));
		}
		System.gc();
		Thread.sleep(3000);
		logMem();
		System.out.println(map.size());
	}

	// @Test
	// public void testMapDbSpeed() {
	// Map<Position, AtomicDouble> map = MapDb.INSTANCE.getDb()
	// .createTreeMap("test").comparator(new PositionComparator())
	// .make();
	// map = new TreeMap<Position, AtomicDouble>();
	// DB db = MapDb.INSTANCE.getDb();
	//
	// // map = new ConcurrentHashMap<Position, AtomicDouble>();
	// long t = System.currentTimeMillis();
	// for (int i = 0; i < 1000000; i++) {
	// Position p = new Position(i / Math.PI, i * Math.PI * Math.E);
	// map.put(p, new AtomicDouble(Math.random() * 800));
	// if (i % 100000 == 0)
	// db.commit();
	// }
	// System.out.println("final commit in "
	// + (System.currentTimeMillis() - t) + "ms");
	// db.commit();
	// System.out.println("done in t=" + (System.currentTimeMillis() - t)
	// + "ms");
	// }

	@Test
	public void dummy() {
		Observable.range(1, 2).lift(new OperatorSkip<Integer>(1)).subscribe();
	}

	private void logMem() {
		System.out.println("mem="
		        + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
	}

}
