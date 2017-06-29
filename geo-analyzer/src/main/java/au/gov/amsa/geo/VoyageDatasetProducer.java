package au.gov.amsa.geo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.guavamini.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;

import au.gov.amsa.geo.distance.EffectiveSpeedCheck;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.Files;
import rx.Observable;

public final class VoyageDatasetProducer {

    private static final String COMMA = ",";
    private static final DateTimeFormatter format = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

    public static void produce() throws Exception {
        File out = new File("target/legs.txt");
        out.delete();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out)))) {
            Pattern pattern = Pattern.compile(".*\\.track");
            List<File> list = new ArrayList<File>();

            String baseFilename = "/media/an/binary-fixes-5-minute/";
            // String baseFilename = "/home/dave/Downloads/";
            list.addAll(Files.find(new File(baseFilename + "2014"), pattern));
            list.addAll(Files.find(new File(baseFilename + "2015"), pattern));
            list.addAll(Files.find(new File(baseFilename + "2016"), pattern));

            int numFiles = list.size();
            System.out.println(numFiles + "binary fix files");

            AtomicInteger fileNumber = new AtomicInteger(0);
            Collection<Port> ports = loadPorts();
            Collection<EezWaypoint> eezWaypoints = readEezWaypoints();
            Shapefile eezLine = loadEezLine();
            Shapefile eezPolygon = loadEezPolygon();
            System.out.println("loaded eez shapefiles");
            long t = System.currentTimeMillis();
            AtomicLong failedCheck = new AtomicLong();
            AtomicLong fixCount = new AtomicLong();
            Map<Integer, Integer> mmsisWithFailedChecks = new TreeMap<>();
            Observable.from(list) //
                    .groupBy(f -> f.getName().substring(0, f.getName().indexOf("."))) //
                    .flatMap(files -> files //
                            .compose(o -> logPercentCompleted(numFiles, t, o, fileNumber)) //
                            .concatMap(BinaryFixes::from) //
                            .lift(new OperatorEffectiveSpeedChecker(SegmentOptions.builder()
                                    .acceptAnyFixHours(24L).maxSpeedKnots(50).build()))
                            .doOnNext(check -> updatedCounts(failedCheck, fixCount,
                                    mmsisWithFailedChecks, check)) //
                            .filter(check -> check.isOk()) //
                            .map(check -> check.fix()) //
                            .compose(o -> toLegs(eezLine, eezPolygon, ports, eezWaypoints, o)) //
                            .filter(x -> includeLeg(x)) //
                            .sorted((a, b) -> compareByMmsiThenLegStartTime(a, b)) //
                            .doOnNext(x -> write(writer, x)) //
            ) //
                    .toBlocking() //
                    .subscribe();
            System.out.println((System.currentTimeMillis() - t) + "ms");
            System.out.println("total fixes=" + fixCount.get());
            System.out.println(
                    "num fixes rejected due failed effective speed check=" + failedCheck.get());
            System.out.println(
                    "num mmsis with failed effective speed checks=" + mmsisWithFailedChecks.size());
            System.out.println("failures mmsi <TAB> number of rejected fixes");
            for (Integer mmsi : mmsisWithFailedChecks.keySet()) {
                System.out.println(mmsi + "\t" + mmsisWithFailedChecks.get(mmsi));
            }
            try (PrintStream p = new PrintStream("target/failures.txt")) {
                for (Integer mmsi : mmsisWithFailedChecks.keySet()) {
                    p.println(mmsi + "\t" + mmsisWithFailedChecks.get(mmsi));
                }
            }
        }
    }

    private static int compareByMmsiThenLegStartTime(TimedLeg x, TimedLeg y) {
        if (x.mmsi == y.mmsi) {
            // compare using just the start time of the leg
            return Long.compare(x.a.time, y.a.time);
        } else {
            return Integer.compare(x.mmsi, y.mmsi);
        }
    }

    private static void updatedCounts(AtomicLong failedCheck, AtomicLong fixCount,
            Map<Integer, Integer> mmsisWithFailedChecks, EffectiveSpeedCheck check) {
        fixCount.incrementAndGet();
        if (!check.isOk()) {
            int count = mmsisWithFailedChecks.getOrDefault(check.fix().mmsi(), 0);
            mmsisWithFailedChecks.put(check.fix().mmsi(), count + 1);
            failedCheck.incrementAndGet();
        }
    }

    private static void write(BufferedWriter writer, TimedLeg x) {
        try {
            writer.write(String.valueOf(x.mmsi));
            writer.write(COMMA);
            writer.write(formatTime(x.a.time));
            writer.write(COMMA);
            writer.write(x.a.waypoint.code());
            writer.write(COMMA);
            writer.write(formatTime(x.b.time));
            writer.write(COMMA);
            writer.write(x.b.waypoint.code());
            writer.write("\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean includeLeg(TimedLeg x) {
        // exclude EEZ -> EEZ
        return !(x.a.waypoint instanceof EezWaypoint && x.b.waypoint instanceof EezWaypoint);
    }

    private static String formatTime(long t) {
        return format.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneOffset.UTC));
    }

    private static Collection<EezWaypoint> readEezWaypoints() throws IOException {
        Collection<EezWaypoint> eezWaypoints;
        try (Reader reader = new InputStreamReader(
                VoyageDatasetProducer.class.getResourceAsStream("/eez-waypoints.csv"))) {
            eezWaypoints = Strings.lines(reader) //
                    .map(line -> line.trim()) //
                    .filter(line -> line.length() > 0) //
                    .filter(line -> !line.startsWith("#")) //
                    .map(line -> line.split(COMMA))
                    .map(items -> new EezWaypoint(items[0], Double.parseDouble(items[2]),
                            Double.parseDouble(items[1]),
                            // TODO read thresholdKm
                            Optional.empty())) //
                    .doOnNext(System.out::println) //
                    .toList() //
                    .toBlocking().single();
        }
        return eezWaypoints;
    }

    static Collection<Port> loadPorts() throws IOException {
        Collection<Port> ports;
        try (Reader reader = new InputStreamReader(
                VoyageDatasetProducer.class.getResourceAsStream("/ports.txt"))) {
            ports = Strings.lines(reader) //
                    .map(line -> line.trim()) //
                    .filter(line -> line.length() > 0) //
                    .filter(line -> !line.startsWith("#")) //
                    .map(line -> line.split("\t"))
                    .map(items -> new Port(items[0], items[1],
                            Shapefile.fromZip(VoyageDatasetProducer.class
                                    .getResourceAsStream("/port-visit-shapefiles/" + items[2])))) //
                    .doOnNext(x -> System.out
                            .println(x.name + " - " + x.visitRegion.contains(-33.8568, 151.2153))) //
                    .toList() //
                    .toBlocking().single();
        }
        return ports;
    }

    static Shapefile loadEezLine() {
        // good for crossing checks
        return Shapefile.fromZip(
                VoyageDatasetProducer.class.getResourceAsStream("/eez_aust_mainland_line.zip"));
    }

    static Shapefile loadEezPolygon() {
        // good for contains checks
        return Shapefile.fromZip(
                VoyageDatasetProducer.class.getResourceAsStream("/eez_aust_mainland_pl.zip"));
    }

    private static Observable<File> logPercentCompleted(int numFiles, long startTime,
            Observable<File> o, AtomicInteger fileNumber) {
        return o.doOnNext(file -> {
            int n = fileNumber.incrementAndGet();
            if (n % 1000 == 0) {
                long t = System.currentTimeMillis();
                long timeRemainingSeconds = Math
                        .round(((double) t - startTime) / n * (numFiles - n)) / 1000;
                System.out.println(
                        "complete: " + new DecimalFormat("0.0").format(n / (double) numFiles * 100)
                                + "%, seconds remaining " + timeRemainingSeconds);
            }
        });
    }

    private enum EezStatus {
        IN, OUT, UNKNOWN;

        public static EezStatus from(boolean inEez) {
            return inEez ? IN : OUT;
        }
    }

    public static final class TimedLeg {

        private static final DateTimeFormatter format = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm");
        public final int mmsi;
        public final TimedWaypoint a;
        public final TimedWaypoint b;

        public TimedLeg(int mmsi, TimedWaypoint a, TimedWaypoint b) {
            Preconditions.checkNotNull(a);
            Preconditions.checkNotNull(b);
            this.mmsi = mmsi;
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return format
                    .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(a.time), ZoneOffset.UTC))
                    + "->"
                    + format.format(
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(b.time), ZoneOffset.UTC))
                    + " " + a.waypoint.name() + "->" + b.waypoint.name();
        }
    }

    private static final class State {
        final TimedWaypoint timedWaypoint; // nullable
        final Fix latestFix; // nullable
        final EezStatus fixStatus; // defaults to UNKNOWN

        State(TimedWaypoint waypoint, Fix latestFix, EezStatus fixStatus) {
            this.timedWaypoint = waypoint;
            this.latestFix = latestFix;
            this.fixStatus = fixStatus;
        }

    }

    @VisibleForTesting
    static Observable<TimedLeg> toLegs(Shapefile eezLine, Shapefile eezPolygon,
            Collection<Port> ports, Collection<EezWaypoint> eezWaypoints, Observable<Fix> fixes) {
        return Observable.defer(() -> //
        {
            State[] state = new State[1];
            state[0] = new State(null, null, EezStatus.UNKNOWN);
            return fixes //
                    .flatMap(fix -> {
                List<TimedLeg> legs = null; // only create when needed to reduce
                                            // allocations
                boolean inEez = eezPolygon.contains(fix.lat(), fix.lon());
                State current = state[0];
                Preconditions.checkArgument(
                        current.latestFix == null || fix.time() >= current.latestFix.time(),
                        "fixes out of time order!");
                boolean crossed = (inEez && current.fixStatus == EezStatus.OUT)
                        || (!inEez && current.fixStatus == EezStatus.IN);
                if (crossed) {
                    TimedWaypoint closestWaypoint = findClosestWaypoint(eezLine, eezWaypoints, fix,
                            current);
                    if (current.timedWaypoint != null) {
                        if (legs == null) {
                            legs = new ArrayList<>(2);
                        }
                        legs.add(new TimedLeg(fix.mmsi(), current.timedWaypoint, closestWaypoint));
                    }
                    current = new State(closestWaypoint, fix, EezStatus.from(inEez));
                }
                // Note that may have detected eez crossing but also
                // have arrived in port so may need to return both
                // waypoints
                if (inEez) {
                    Optional<Port> port = findPort(ports, fix.lat(), fix.lon());
                    if (port.isPresent()) {
                        TimedWaypoint portTimedWaypoint = new TimedWaypoint(port.get(), fix.time());
                        state[0] = new State(portTimedWaypoint, fix, EezStatus.IN);
                        if (current.fixStatus != EezStatus.UNKNOWN && current.timedWaypoint != null
                                && current.timedWaypoint.waypoint != port.get()) {
                            if (current.timedWaypoint != null) {
                                if (legs == null) {
                                    legs = new ArrayList<>(2);
                                }
                                legs.add(new TimedLeg(fix.mmsi(), current.timedWaypoint,
                                        portTimedWaypoint));
                            }
                        }
                    } else {
                        state[0] = new State(current.timedWaypoint, fix, EezStatus.IN);
                    }
                } else {
                    state[0] = new State(current.timedWaypoint, fix, EezStatus.OUT);
                }
                if (legs == null) {
                    return Observable.empty();
                } else {
                    return Observable.from(legs);
                }

            });
        });

    }

    private static TimedWaypoint findClosestWaypoint(Shapefile eezLine,
            Collection<EezWaypoint> eezWaypoints, Fix fix, State previous) {
        TimedPosition crossingPoint = findRegionCrossingPoint(eezLine, previous.latestFix, fix);
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
        return new TimedWaypoint(closest, crossingPoint.time);
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

    public static interface Waypoint {
        String name();

        String code();
    }

    @VisibleForTesting
    public static final class EezWaypoint implements Waypoint {
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

        @Override
        public String toString() {
            return "EezWaypoint [name=" + name + ", lat=" + lat + ", lon=" + lon + ", thresholdKm="
                    + thresholdKm + "]";
        }

        @Override
        public String code() {
            return name();
        }

    }

    @VisibleForTesting
    public static final class Port implements Waypoint {
        public final String name;
        public final String code;
        public final Shapefile visitRegion;

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
            return "Port [name=" + name + "]";
        }

        @Override
        public String code() {
            return code;
        }

    }

    private static TimedPosition findRegionCrossingPoint(Shapefile region, Fix fix1, Fix fix2) {

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
                    return new TimedPosition(fix1.lat(), fix1.lon(), fix1.time());
                } else if (bc == 0) {
                    return new TimedPosition(fix2.lat(), fix2.lon(), fix2.time());
                } else {
                    // predict the timestamp based on distance from a and b
                    long diff = fix2.time() - fix1.time();
                    long t = Math.round(fix1.time() + ac * diff / (ac + bc));
                    return new TimedPosition(latitude, longitude, t);
                }
            }
        }
        throw new RuntimeException("crossing not found");
    }

    private static final class TimedPosition {
        final double lat;
        final double lon;
        final long time;

        TimedPosition(double lat, double lon, long time) {
            this.lat = lat;
            this.lon = lon;
            this.time = time;
        }
    }

    @VisibleForTesting
    public static final class TimedWaypoint {
        public final Waypoint waypoint;
        public final long time;

        TimedWaypoint(Waypoint waypoint, long time) {
            this.waypoint = waypoint;
            this.time = time;
        }

        @Override
        public String toString() {
            return "TimedWaypoint [waypoint=" + waypoint + ", time=" + time + "]";
        }

    }

    public static void main(String[] args) throws Exception {
        VoyageDatasetProducer.produce();
    }
}
