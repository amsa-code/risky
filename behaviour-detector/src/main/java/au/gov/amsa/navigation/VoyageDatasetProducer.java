package au.gov.amsa.navigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.github.davidmoten.grumpy.core.Position;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;

import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.Files;
import rx.Observable;

public final class VoyageDatasetProducer {

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
            Collection<Port> ports;
            try (Reader reader = new InputStreamReader(VoyageDatasetProducer.class.getResourceAsStream("/ports.txt"))) {
                ports = Strings.lines(reader) //
                        .map(line -> line.trim()) //
                        .filter(line -> line.length() > 0) //
                        .filter(line -> !line.startsWith("#")) //
                        .map(line -> line.split("\t"))
                        .map(items -> new Port(items[0], items[1],
                                Shapefile.fromZip(VoyageDatasetProducer.class
                                        .getResourceAsStream("/port-visit-shapefiles/" + items[2])))) //
                        .doOnNext(System.out::println) //
                        .doOnNext(x -> System.out.println(x.name + " - " + x.visitRegion.contains(-33.8568, 151.2153))) //
                        .toList() //
                        .toBlocking().single();
            }
            Shapefile eez = Shapefile
                    .fromZip(VoyageDatasetProducer.class.getResourceAsStream("/eez_aust_mainland_pl.zip"));
            System.out.println("read eez shapefile");
            System.out.println(eez.contains(-35, 149));

            Set<EezWaypoint> eezWaypoints = Collections.emptySet();
            Observable.from(list) //
                    // .groupBy(f -> count.getAndIncrement() %
                    // Runtime.getRuntime().availableProcessors()) //
                    .groupBy(f -> f.getName().substring(0, f.getName().indexOf("."))) //
                    .flatMap(files -> files // s
                            .compose(o -> logPercentCompleted(numFiles, o, fileNumber)) //
                            .concatMap(BinaryFixes::from) //
                            .compose(o -> toWaypoints(eez, ports, eezWaypoints, o)) //
                    // .filter(x -> inGbr(x)) //
                    // .onBackpressureBuffer() //
                    // .subscribeOn(Schedulers.computation()) //
                    ) //
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

    private static Observable<Waypoint> toWaypoints(Shapefile eez, Collection<Port> ports,
            Collection<EezWaypoint> eezWaypoints, Observable<Fix> fixes) {
        return Observable.defer(() -> //
        {
            State[] state = new State[1];
            state[0] = new State(EezStatus.UNKNOWN, null);
            return fixes //
                    .flatMap(fix -> {
                        List<Waypoint> results = new ArrayList<>();
                        boolean inEez = eez.contains(fix.lat(), fix.lon());
                        State previous = state[0];
                        long intervalMs = fix.time() - previous.time();
                        Preconditions.checkArgument(intervalMs >= 0, "fixes out of time order!");
                        boolean previousIsRecent = intervalMs <= FIX_AGE_THRESHOLD_MS;
                        boolean crossed = (inEez && previous.eez == EezStatus.OUT)
                                || (!inEez && previous.eez == EezStatus.IN);
                        if (previousIsRecent && crossed) {
                            TimestampedPosition crossingPoint = findRegionCrossingPoint(eez, previous.fix, fix);
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
                        state[0] = new State(inEez ? EezStatus.IN : EezStatus.OUT, fix);
                        return Observable.from(results);
                    });
        });

    }

    private static Optional<Port> findPort(Collection<Port> ports, float lat, float lon) {
        for (Port port : ports) {
            if (port.visitRegion.contains(lat, lon)) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
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
        final String code;
        final Shapefile visitRegion;

        Port(String name, String code, Shapefile visitRegion) {
            this.name = name;
            this.code = code;
            this.visitRegion = visitRegion;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Port [name=" + name + ", visitRegion=" + visitRegion + "]";
        }

    }

    private static TimestampedPosition findRegionCrossingPoint(Shapefile region, Fix fix1, Fix fix2) {

        Coordinate[] coords = new Coordinate[] { new Coordinate(fix1.lon(), fix1.lat()),
                new Coordinate(fix2.lon(), fix2.lat()) };
        LineString line = new GeometryFactory().createLineString(coords);
        for (PreparedGeometry g : region.geometries()) {
            if (g.crosses(line)) {
                Geometry intersection = g.getGeometry().intersection(line);
                // expecting just one point
                Coordinate coord = intersection.getCoordinate();
                double longitude = coord.x;
                double latitude = coord.y;
                Position a = Position.create(fix1.lat(), fix1.lon());
                Position b = Position.create(fix2.lat(), fix2.lon());
                Position c = Position.create(latitude, longitude);
                double ac = a.getDistanceToKm(c);
                double bc = b.getDistanceToKm(c);
                if (ac == 0) {
                    return new TimestampedPosition(fix1.lat(), fix1.lon(), fix1.time());
                } else if (bc == 0) {
                    return new TimestampedPosition(fix2.lat(), fix2.lon(), fix2.time());
                } else {
                    // predict the timestamp based on distance from a and b
                    long diff = fix2.time() - fix1.time();
                    long t = Math.round(fix1.time() + (ac * diff + bc * diff) / (ac + bc));
                    return new TimestampedPosition(latitude, longitude, t);
                }
            }
        }
        throw new RuntimeException("crossing not found");
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

    private static boolean inGbr(Fix fix) {
        return fix.lat() >= -27.8 && fix.lat() <= -8.4 && fix.lon() >= 142 && fix.lon() <= 162;
    }

    public static void main(String[] args) throws Exception {
        VoyageDatasetProducer.produce();
    }
}
