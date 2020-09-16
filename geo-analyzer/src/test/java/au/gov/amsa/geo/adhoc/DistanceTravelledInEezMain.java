package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.github.davidmoten.grumpy.core.Position;

import au.gov.amsa.geo.Eez;
import au.gov.amsa.geo.ShapefileUtil;
import au.gov.amsa.geo.TimedPosition;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.identity.MmsiValidator2;
import rx.Observable;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

public class DistanceTravelledInEezMain {

    private static final int MIN_DISTANCE_KM_TO_ESTIMATE_TIME = 1;
    private static final Logger log = Logger.getLogger(DistanceTravelledInEezMain.class);

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

        public String formattedDate() {
            return DistanceTravelledInEezMain.formattedDate(date);
        }
        
    }

    static String formattedDate(String date) {
        SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd");
        sdfIn.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date d = sdfIn.parse(date);
            // Sabine's preferred date format for importing into SPSS
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd-MMM-yyyy");
            sdfOut.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdfOut.format(d);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        System.out.println("running");

        File tracks = new File("/home/dxm/combinedSortedTracks");
        long t = System.currentTimeMillis();
        List<File> files = Arrays.asList(tracks.listFiles());
        files.sort((a, b) -> a.getName().compareTo(b.getName()));
        try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream("target/output.csv")))) {
            out.println(Vessel.headings());
            Observable //
                    .from(files) //
                    .filter(x -> x.getName().endsWith(".track.gz")) //
                    .filter(x -> x.getName().startsWith("2020")) //
                    .flatMap(file -> {
                        log.info(file);

                        // Note that the Shapefile objects are not thread-safe so we make new one for   
                        // each file to enable parallel processing

                        // used for intersections with eez boundary
                        Shapefile eezLine = Eez.loadEezLine();

                        // used for contains tests
                        Shapefile eezPolygon = Eez.loadEezPolygon();
                        long startTime = Util.getStartTime(file);
                        long endTime = startTime + TimeUnit.HOURS.toMillis(24);
                        return BinaryFixes //
                                .from(file, true, BinaryFixesFormat.WITH_MMSI) //
                                .subscribeOn(Schedulers.computation()) //
                                .filter(f -> MmsiValidator2.INSTANCE.isValid(f.mmsi())) //
                                .filter(f -> f.time() >= startTime && f.time() <= endTime) //
                                .groupBy(fix -> fix.mmsi()) //
                                .flatMap(o -> calculateDistance(file, eezLine, eezPolygon, o));
                    }, Runtime.getRuntime().availableProcessors()) //
                    .filter(x -> x.state.fix != null) //
                    .toBlocking() //
                    .forEach(x -> out.println(x.line()));
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static Observable<? extends Vessel> calculateDistance(File file, Shapefile eezLine, Shapefile eezPolygon,
            GroupedObservable<Integer, Fix> o) {
        return Observable.defer(() -> {
            State state = new State();
            state.date = file.getName().substring(0, file.getName().indexOf(".track.gz"));
            state.mmsi = o.getKey();
            state.location = Location.UNKNOWN;
            return o //
                    .compose(Downsample.minTimeStep(5, TimeUnit.MINUTES)) //
                    .lift(new OperatorEffectiveSpeedChecker(
                            SegmentOptions.builder().acceptAnyFixHours(480L).maxSpeedKnots(50).build()))
                    .filter(check -> check.isOk()) //
                    .map(check -> check.fix()) //
                    .doOnNext(fix -> {
                        //TODO unit test
                        boolean inside = eezPolygon.contains(fix.lat(), fix.lon());
                        Location location = inside ? Location.IN : Location.OUT;
                        if (state.location != Location.UNKNOWN) {
                            boolean crossed = state.location != location;
                            if (crossed) {
                                TimedPosition point = ShapefileUtil.findRegionCrossingPoint(eezLine, state.fix, fix);
                                final double distance;
                                if (location == Location.IN) {
                                    distance = distanceKm(fix.lat(), fix.lon(), point.lat, point.lon);
                                } else {
                                    distance = distanceKm(state.fix.lat(), state.fix.lon(), point.lat, point.lon);
                                }
                                state.distanceKm += distance;
                                double d = distanceKm(state.fix.lat(), state.fix.lon(), fix.lat(), fix.lon());
                                if (d >= MIN_DISTANCE_KM_TO_ESTIMATE_TIME) {
                                    // we ensure that d is not close to zero so that the time estimate does not get
                                    // blown out by instability in the division.
                                    state.totalTimeMs += distance / d * (fix.time() - state.fix.time());
                                }
                            } else if (location == Location.IN) {
                                state.distanceKm += distanceKm(state.fix.lat(), state.fix.lon(), fix.lat(), fix.lon());
                                state.totalTimeMs += fix.time() - state.fix.time();
                            }
                        }
                        state.fix = fix;
                        state.location = location;
                    }) //
                    .count() //
                    .map(count -> new Vessel(count, state));
        });
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

        static String headings() {
            return "dateUTC,mmsi,aisClass,numReports,distanceNmInEEZ,elapsedTimeHoursInEEZ";
        }

        String line() {
            DecimalFormat df = new DecimalFormat("0.000");
            return String.format("%s,%s,%s,%s,%s,%s", //
                    state.formattedDate(), //
                    state.mmsi, //
                    state.fix.aisClass(), //
                    count, //
                    df.format(state.distanceKm / 1.852), //
                    df.format(state.totalTimeHours()));
        }

    }

    private static double distanceKm(double lat, double lon, double lat2, double lon2) {
        return Position.create(lat, lon).getDistanceToKm(Position.create(lat2, lon2));
    }

}
