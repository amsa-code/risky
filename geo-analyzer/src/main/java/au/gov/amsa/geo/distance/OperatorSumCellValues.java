package au.gov.amsa.geo.distance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.Position;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;

public class OperatorSumCellValues implements Operator<CellValue, CellValue> {

    private static Logger log = LoggerFactory.getLogger(OperatorSumCellValues.class);

    private static final int INITIAL_CAPACITY = 35000000;

    /**
     * This takes about 100 bytes per entry of memory;
     */
    final Map<Position, Double> map;

    @SuppressWarnings("unchecked")
    public OperatorSumCellValues(boolean useDisk) {
        if (useDisk)
            map = (Map<Position, Double>) MapDb.INSTANCE.getDb().hashMap(UUID.randomUUID().toString()).create();
        else
            map = new HashMap<Position, Double>(INITIAL_CAPACITY, 1.0f);
    }

    public OperatorSumCellValues() {
        this(false);
    }

    // private final AtomicLong count = new AtomicLong();

    @Override
    public Subscriber<? super CellValue> call(final Subscriber<? super CellValue> child) {

        Subscriber<CellValue> parent = Subscribers.from(new Observer<CellValue>() {

            @Override
            public void onCompleted() {
                // MapDb.INSTANCE.getDb().commit();
                try {
                    log.info("starting to emit map values");
                    synchronized (map) {
                        for (Entry<Position, Double> entry : map.entrySet()) {
                            CellValue cv = new CellValue(entry.getKey().lat(),
                                    entry.getKey().lon(), entry.getValue().doubleValue());
                            child.onNext(cv);
                        }
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
            public void onNext(CellValue cv) {
                Position position = new Position((float) cv.getCentreLat(), (float) cv
                        .getCentreLon());
                Double val = map.putIfAbsent(position, cv.getValue());
                if (val != null)
                    map.put(position, val + cv.getValue());
            }
        });
        child.add(parent);
        return parent;
    }
}
