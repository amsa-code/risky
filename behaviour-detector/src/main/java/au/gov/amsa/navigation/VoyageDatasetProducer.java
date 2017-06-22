package au.gov.amsa.navigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.github.davidmoten.grumpy.core.Position;
import com.google.common.base.Preconditions;

import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import rx.Observable;

public class VoyageDatasetProducer {
    public static void produce() throws Exception {
        long t = System.currentTimeMillis();
        File out = new File("target/positions.txt");
        out.delete();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)))) {
            Pattern pattern = Pattern.compile(".*\\.track");
            List<File> list = new ArrayList<File>();

            // list.addAll(Files.find(new
            // File("/media/an/binary-fixes-5-minute/2014"), pattern));
            // list.addAll(Files.find(new
            // File("/media/an/binary-fixes-5-minute/2015"), pattern));
            list.addAll(Files.find(new File("/home/dave/Downloads/2016"), pattern));
            AtomicInteger count = new AtomicInteger();

            int numFiles = list.size();
            System.out.println(numFiles + " files");

            AtomicInteger fileNumber = new AtomicInteger(0);
            Observable.from(list) //
                    // .groupBy(f -> count.getAndIncrement() %
                    // Runtime.getRuntime().availableProcessors()) //
                    .groupBy(f -> f.getName().substring(0, f.getName().indexOf("."))) //
                    .flatMap(files -> files // s
                            .compose(o -> logPercentCompleted(numFiles, o, fileNumber)) //
                            .concatMap(BinaryFixes::from) //
                            .compose(o -> toWaypoints(o)) //
                    // .filter(x -> inGbr(x)) //
                    // .onBackpressureBuffer() //
                    // .subscribeOn(Schedulers.computation()) //
                    ) //
                    .count() //
                    .doOnNext(System.out::println) //
                    .toBlocking() //
                    .subscribe();
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static Observable<File> logPercentCompleted(int numFiles, Observable<File> o, AtomicInteger fileNumber) {
        return o.doOnNext(file -> {
            int n = fileNumber.incrementAndGet();
            if (n % 1000 == 0)
                System.out.println("complete: " + new DecimalFormat("0.0").format(n / (double) numFiles * 100) + "%");
        });
    }

    private enum EezStatus {
        IN, OUT, UNKNOWN;
    }

    private static final class State {
        final EezStatus eez;
        final Fix fix;

        long time() {
            if (fix != null) {
                return fix.time();
            } else
                return 0;
        }

        State(EezStatus eez, Fix fix) {
            this.eez = eez;
            this.fix = fix;
        }
    }

    private static final long FIX_AGE_THRESHOLD_MS = TimeUnit.DAYS.toMillis(5);

    private static Observable<Waypoint> toWaypoints(Set<Port> ports, Set<EezWaypoint> eezWaypoints,
            Observable<Fix> fixes) {
        return Observable.defer(() -> //
        {
            State[] state = new State[1];
            state[0] = new State(EezStatus.UNKNOWN, null);
            return fixes //
                    .flatMap(fix -> {
                        List<Waypoint> results = new ArrayList<>();
                        boolean inEez = inEez(fix);
                        State previous = state[0];
                        long intervalMs = fix.time() - previous.time();
                        Preconditions.checkArgument(intervalMs >= 0, "fixes out of time order!");
                        boolean previousIsRecent = intervalMs <= FIX_AGE_THRESHOLD_MS;
                        boolean crossed = (inEez && previous.eez == EezStatus.OUT)
                                || (!inEez && previous.eez == EezStatus.IN);
                        if (previousIsRecent && crossed) {
                            TimestampedPosition crossingPoint = findEezCrossingPoint(previous, fix);
                            EezWaypoint closest = null;
                            double closestDistanceKm = 0;
                            for (EezWaypoint w : eezWaypoints) {
                                double d = distanceKm(crossingPoint.lat, crossingPoint.lon, w.lat, w.lon);
                                if (closest == null
                                        || (d < closestDistanceKm && d <= w.thresholdKm.orElse(Double.MAX_VALUE))) {
                                    closest = w;
                                    closestDistanceKm = d;
                                }
                            }
                            Preconditions.checkNotNull(closest, "no eez waypoint found!");
                            results.add(closest);
                        }
                        // Note that may have detected eez crossing but also
                        // have arrived in port so may need to return both
                        // waypoints
                        if (inEez) {
                            Optional<Port> port = findPort(ports, fix.lat(), fix.lon());
                            if (port.isPresent()) {
                                results.add(port.get());
                            }
                        }
                        return Observable.from(results);
                    });
        });

    }

    private static Optional<Port> findPort(Set<Port> ports, float lat, float lon) {
        // TODO
        return null;
    }

    private static double distanceKm(double lat, double lon, double lat2, double lon2) {
        return Position.create(lat, lon).getDistanceToKm(Position.create(lat2, lon2));
    }

    private static interface Waypoint {
        String name();
    }

    private static final class EezWaypoint implements Waypoint {
        final String name;
        final double lat;
        final double lon;
        final Optional<Double> thresholdKm;

        EezWaypoint(String name, double lat, double lon, Optional<Double> thresholdKm) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.thresholdKm = thresholdKm;
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static final class Port implements Waypoint {
        final String name;
        final Shapefile visitRegion;

        Port(String name, Shapefile visitRegion) {
            this.name = name;
            this.visitRegion = visitRegion;
        }

        @Override
        public String name() {
            return name;
        }

    }

    private static TimestampedPosition findEezCrossingPoint(State previous, Fix fix) {
        // TODO
        return null;
    }

    private static final class TimestampedPosition {
        final double lat;
        final double lon;
        final long time;

        TimestampedPosition(double lat, double lon, long time) {
            this.lat = lat;
            this.lon = lon;
            this.time = time;
        }
    }

    private static boolean inEez(Fix fix) {
        // TODO
        return true;
    }

    private static boolean inGbr(Fix fix) {
        return fix.lat() >= -27.8 && fix.lat() <= -8.4 && fix.lon() >= 142 && fix.lon() <= 162;
    }

    public static void main(String[] args) throws Exception {
        VoyageDatasetProducer.produce();
    }
}
