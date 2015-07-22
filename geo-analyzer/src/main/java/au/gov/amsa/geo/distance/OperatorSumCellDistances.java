package au.gov.amsa.geo.distance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.geo.model.Cell;

public final class OperatorSumCellDistances implements Operator<Map<Cell, Double>, CellAndDistance> {

    private static final int INITIAL_CAPACITY = 100000;

    private static Logger log = Logger.getLogger(OperatorSumCellDistances.class);

    /**
     * This takes about 100 bytes per entry of memory;
     */
    private Map<Cell, Double> map = createMap();

    private final int maxSize;

    private OperatorSumCellDistances(int maxSize) {
        this.maxSize = maxSize;
    }

    public static OperatorSumCellDistances create(int maxSize) {
        return new OperatorSumCellDistances(maxSize);
    }

    @Override
    public Subscriber<? super CellAndDistance> call(
            final Subscriber<? super Map<Cell, Double>> child) {

        Subscriber<CellAndDistance> parent = new Subscriber<CellAndDistance>(child) {

            @Override
            public void onCompleted() {
                try {
                    child.onNext(Collections.unmodifiableMap(map));
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
                Cell key = cd.getCell();

                Double val = map.putIfAbsent(key, cd.getDistanceNm());
                if (val != null) {
                    map.put(key, val + cd.getDistanceNm());
                    request(1);
                } else {
                    if (map.size() == maxSize) {
                        Map<Cell, Double> m = map;
                        map = createMap();
                        child.onNext(Collections.unmodifiableMap(m));
                    } else
                        request(1);
                }
            }
        };
        return parent;
    }

    private static Map<Cell, Double> createMap() {
        return new HashMap<Cell, Double>(INITIAL_CAPACITY, 1.0f);
    }
}
