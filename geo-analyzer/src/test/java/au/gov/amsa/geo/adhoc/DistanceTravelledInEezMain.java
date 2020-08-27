package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

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

        private static final long HOUR_MILLIS = TimeUnit.HOURS.toMillis(1);

        String date;

        int mmsi;
        Fix fix;
        Location location;
        double distanceKm;

        double totalTimeMs;

        public double totalTimeHours() {
            return totalTimeMs / HOUR_MILLIS;
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Shapefile eezLine = Eez.loadEezLine();
        Shapefile eezPolygon = Eez.loadEezPolygon();
        File tracks = new File("/home/dxm/combinedSortedTracks");
        long t = System.currentTimeMillis();
        DecimalFormat df = new DecimalFormat("0.00");
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream("target/output.csv"))) {
            Observable //
                    .from(tracks.listFiles()) //
                    .filter(x -> x.getName().endsWith("2019-01-01.track.gz")) //
                    .flatMap(file -> BinaryFixes //
                            .from(file, true, BinaryFixesFormat.WITH_MMSI) //
                            .filter(f -> MmsiValidator2.INSTANCE.isValid(f.mmsi())) //
                            .groupBy(fix -> fix.mmsi()) //
                            .flatMap(o -> {
                                State state = new State();
                                state.date = file.getName().substring(0, file.getName().indexOf(".track.gz"));
                                state.mmsi = o.getKey();
                                state.location = Location.UNKNOWN;
                                return o //
                                        .lift(new OperatorEffectiveSpeedChecker(SegmentOptions.builder()
                                                .acceptAnyFixHours(12L).maxSpeedKnots(50).build()))
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
                                                        distance = distanceKm(fix.lat(), fix.lon(), point.lat,
                                                                point.lon);
                                                    } else {
                                                        distance = distanceKm(state.fix.lat(), state.fix.lon(),
                                                                point.lat, point.lon);
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
                                        .map(count -> new Vessel(count, state));
                            })) //
                    .forEach(x -> System.out.println(x.line()));
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    static final class Vessel {
        final long count;

        State state;

        double totalTimeHours() {
            return state.totalTimeMs / TimeUnit.HOURS.toMillis(1);
        }

        Vessel(long count, State state) {
            this.count = count;
            this.state = state;
        }

        String line() {
            DecimalFormat df = new DecimalFormat("0.000");
            return String.format("%s,%s,%s,%s,%s", //
                    state.mmsi, //
                    state.date, //
                    count, //
                    df.format(state.distanceKm / 1.852), //
                    df.format(state.totalTimeHours()));
        }

    }

    private static double distanceKm(double lat, double lon, double lat2, double lon2) {
        return Position.create(lat, lon).getDistanceToKm(Position.create(lat2, lon2));
    }

}
