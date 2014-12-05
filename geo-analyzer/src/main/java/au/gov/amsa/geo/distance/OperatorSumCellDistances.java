package au.gov.amsa.geo.distance;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;
import au.gov.amsa.geo.Util;
import au.gov.amsa.geo.model.Cell;

import com.google.common.util.concurrent.AtomicDouble;

public class OperatorSumCellDistances implements
		Operator<Map<Cell, AtomicDouble>, CellAndDistance> {

	private static final int INITIAL_CAPACITY = 100000000;

	private static Logger log = Logger
			.getLogger(OperatorSumCellDistances.class);

	private long count = 0;

	/**
	 * This takes about 100 bytes per entry of memory;
	 */
	private final Map<Cell, AtomicDouble> map = new ConcurrentHashMap<Cell, AtomicDouble>(
			INITIAL_CAPACITY, 1.0f);

	@Override
	public Subscriber<? super CellAndDistance> call(
			final Subscriber<? super Map<Cell, AtomicDouble>> child) {

		Subscriber<CellAndDistance> parent = Subscribers
				.from(new Observer<CellAndDistance>() {

					@Override
					public void onCompleted() {
						try {
							log.info("finished a parallel fork");
							synchronized (map) {
								child.onNext(Collections.unmodifiableMap(map));
							}
							child.onCompleted();
						} catch (Throwable t) {
							onError(t);
						}
					}

					@Override
					public void onError(Throwable e) {
						child.onError(e);
					}

					@Override
					public void onNext(CellAndDistance cd) {
						long n = ++count;
						if (n % 1000000 == 0)
							log.info("cells received " + n / 1000000
									+ "m mapSize=" + (map.size() / 1000000.0)
									+ "m, " + Util.memoryUsage());
						Cell key = cd.getCell();
						AtomicDouble val = map.get(key);
						// double checked locking pattern
						if (val == null)
							synchronized (map) {
								val = map.get(key);
								if (val == null)
									map.put(key,
											new AtomicDouble(cd.getDistanceNm()));
								else
									val.addAndGet(cd.getDistanceNm());
							}
						else
							val.addAndGet(cd.getDistanceNm());
					}
				});
		child.add(parent);
		return parent;
	}
}
