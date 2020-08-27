package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.text.DecimalFormat;

import com.github.davidmoten.grumpy.core.Position;

import au.gov.amsa.geo.Eez;
import au.gov.amsa.geo.ShapefileUtil;
import au.gov.amsa.geo.TimedPosition;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.identity.MmsiValidator2;
import rx.Observable;

public class DistanceTravelledInEezMain {

    private enum Location {
        IN, OUT, UNKNOWN;
    }

    private static final class State {
        Fix fix;
        Location location;
        double distanceKm;

        double totalTimeMs;
    }

    public static void main(String[] args) {
        Shapefile eezLine = Eez.loadEezLine();
        Shapefile eezPolygon = Eez.loadEezPolygon();
        File tracks = new File("/home/dxm/combinedSortedTracks");
        long t = System.currentTimeMillis();
        DecimalFormat df = new DecimalFormat("0.00");
        Observable //
                .from(tracks.listFiles()) //
                .filter(x -> x.getName().endsWith("2019-01-01.track.gz")) //
                .flatMap(x -> BinaryFixes //
                        .from(x, true, BinaryFixesFormat.WITH_MMSI) //
                        .filter(f -> MmsiValidator2.INSTANCE.isValid(f.mmsi())) //
                        .groupBy(fix -> fix.mmsi()) //
                        .flatMap(o -> {
                            State state = new State();
                            state.location = Location.UNKNOWN;
                            return o //
                                    .lift(new OperatorEffectiveSpeedChecker(
                                            SegmentOptions.builder().acceptAnyFixHours(12L).maxSpeedKnots(50).build()))
                                    .filter(check -> check.isOk()) //
                                    .map(check -> check.fix()) //
                                    //
                                    .doOnNext(fix -> {
                                        boolean inside = eezPolygon.contains(fix.lat(), fix.lon());
                                        Location location = inside ? Location.IN : Location.OUT;
                                        if (state.location != Location.UNKNOWN) {
                                            boolean crossed = state.location != location;
                                            if (crossed) {
                                                TimedPosition point = ShapefileUtil.findRegionCrossingPoint(eezLine,
                                                        state.fix, fix);
                                                final double distance;
                                                if (state.location == Location.IN) {
                                                    distance = distanceKm(fix.lat(), fix.lon(), point.lat, point.lon);
                                                } else {
                                                    distance = distanceKm(state.fix.lat(), state.fix.lon(), point.lat,
                                                            point.lon);
                                                }
                                                state.distanceKm += distance;
                                                state.totalTimeMs += distance / distanceKm(state.fix.lat(),
                                                        state.fix.lon(), fix.lat(), fix.lon())
                                                        * (fix.time() - state.fix.time());
                                            } else {
                                                state.distanceKm += distanceKm(state.fix.lat(), state.fix.lon(),
                                                        fix.lat(), fix.lon());
                                                state.totalTimeMs += fix.time() - state.fix.time();
                                            }
                                        }
                                        state.fix = fix;
                                        state.location = location;
                                    }).count() //
                                    .map(f -> new Vessel(o.getKey(), f, state.distanceKm, state.totalTimeMs));
                        })) //
                .forEach(x -> System.out.println(x.mmsi + ": " + x.count + ", kmInEez=" + df.format(x.distanceKm) + ", timeHoursInEez=" + x.totalTimeMs));
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    static final class Vessel {
        final int mmsi;
        final long count;
        double distanceKm;
        
        double totalTimeMs;

        Vessel(int mmsi, long count, double distanceKm, double totalTimeMs) {
            this.mmsi = mmsi;
            this.count = count;
            this.distanceKm = distanceKm;
            this.totalTimeMs = totalTimeMs;
        }

    }

    private static double distanceKm(double lat, double lon, double lat2, double lon2) {
        return Position.create(lat, lon).getDistanceToKm(Position.create(lat2, lon2));
    }

}
