package au.gov.amsa.craft.analyzer.wms;

import static java.util.Optional.empty;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.davidmoten.grumpy.core.Position;

import au.gov.amsa.navigation.Identifier;
import au.gov.amsa.navigation.VesselPosition;
import rx.Observable.Operator;
import rx.Subscriber;

public class OperatorDriftDistanceCheck implements Operator<VesselPosition, VesselPosition> {

    protected static final double MIN_DISTANCE_THRESHOLD_KM = 5 * 1.852;

    @Override
    public Subscriber<? super VesselPosition> call(final Subscriber<? super VesselPosition> child) {
        return new Subscriber<VesselPosition>(child) {

            Optional<Position> min = empty();
            Optional<Position> max = empty();
            Optional<Identifier> id = empty();
            List<VesselPosition> buffer = new LinkedList<VesselPosition>();

            @Override
            public void onCompleted() {
                buffer.clear();
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                buffer.clear();
                child.onError(e);
            }

            @Override
            public void onNext(VesselPosition vp) {
                if (!id.isPresent()
                        || (id.isPresent() && vp.id().uniqueId() != id.get().uniqueId())
                        || vp.data().get().equals(vp.time())) {
                    min = Optional.of(Position.create(vp.lat(), vp.lon()));
                    max = Optional.of(Position.create(vp.lat(), vp.lon()));
                    id = Optional.of(vp.id());
                    buffer.clear();
                } else {
                    min = Optional.of(min(min.get(), Position.create(vp.lat(), vp.lon())));
                    max = Optional.of(max(max.get(), Position.create(vp.lat(), vp.lon())));
                }
                buffer.add(vp);
                if (distanceKm(min.get(), max.get()) >= MIN_DISTANCE_THRESHOLD_KM) {
                    for (VesselPosition p : buffer) {
                        child.onNext(p);
                    }
                    buffer.clear();
                }
            }

        };
    }

    private double distanceKm(Position a, Position b) {
        return a.getDistanceToKm(b);
    }

    private static Position min(Position a, Position b) {
        return Position.create(Math.min(a.getLat(), b.getLat()), Math.min(a.getLon(), b.getLon()));
    }

    private static Position max(Position a, Position b) {
        return Position.create(Math.max(a.getLat(), b.getLat()), Math.max(a.getLon(), b.getLon()));
    }
}
