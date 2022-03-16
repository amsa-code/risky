package au.gov.amsa.geo;

import static au.gov.amsa.geo.VoyageDatasetProducer.compareByMmsiThenLegStartTime;
import static au.gov.amsa.geo.VoyageDatasetProducer.includeLeg;
import static au.gov.amsa.geo.VoyageDatasetProducer.loadPorts;
import static au.gov.amsa.geo.VoyageDatasetProducer.readEezWaypoints;
import static au.gov.amsa.geo.VoyageDatasetProducer.toLegs;
import static au.gov.amsa.geo.VoyageDatasetProducer.updatedCounts;
import static au.gov.amsa.geo.VoyageDatasetProducer.write;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.rx.Checked;
import com.github.davidmoten.rx.Transformers;

import au.gov.amsa.geo.VoyageDatasetProducer.EezWaypoint;
import au.gov.amsa.geo.VoyageDatasetProducer.Persister;
import au.gov.amsa.geo.VoyageDatasetProducer.Port;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Strings;
import rx.Observable;

public final class VoyageDatasetProducer2 {

    public static void produce(File output, File fixesOutput, List<File> list) throws Exception {
        // reset output directories
        output.delete();

        int numFiles = list.size();
        System.out.println("input files count = " + numFiles);

        Collection<Port> ports = loadPorts();
        Collection<EezWaypoint> eezWaypoints = readEezWaypoints();
        Shapefile eezLine = Eez.loadEezLine();
        Shapefile eezPolygon = Eez.loadEezPolygon();
        System.out.println("loaded eez shapefiles");
        long t = System.currentTimeMillis();
        AtomicLong failedCheck = new AtomicLong();
        AtomicLong fixCount = new AtomicLong();
        Map<Integer, Integer> mmsisWithFailedChecks = new TreeMap<>();
        Persister persister = new Persister(fixesOutput);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)))) {

            long[] count = new long[1];
            // Note that in the observable below we don't employ parallel techniques
            // this is because the runtime is acceptable
            int[] currentMmsi = new int[1];
            currentMmsi[0] = -1;
            long[] badItemCount = new long[1];

            Observable.from(list) //
                    .flatMap(file -> //
                    gunzippedLines(file)
                            // skip header
                            .skip(1) //
                            .map(x -> x.trim()) //
                            .filter(x -> !x.isEmpty()) //
                            .map(x -> x.split(",")) //
                            .map(items -> toFix(items)) //
                            .filter(fix -> fix != BAD_FIX) //
//                            .filter(fix -> !eezPolygon.contains(fix.lat(), fix.lon())) //
                            .doOnNext(line -> {
                                count[0]++;
                                if (count[0] % 100000 == 0) {
                                    System.out.println("read " + count[0] / 1000000.0 + "m lines");
                                }
                            }) //
                            .compose(Transformers.bufferWhile(x -> {
                                boolean b = x.mmsi() != currentMmsi[0] && currentMmsi[0] != -1;
                                currentMmsi[0] = x.mmsi();
                                return b;
                            })) //
                            .doOnNext(x -> Preconditions
                                    .checkArgument(x.stream().map(y -> y.mmsi()).distinct().count() == 1, x.toString()))
                            .flatMap(fixes -> Observable.from(fixes) //
                                    .lift(new OperatorEffectiveSpeedChecker(
                                            SegmentOptions.builder().acceptAnyFixHours(24L).maxSpeedKnots(50).build()))
                                    .doOnNext(
                                            check -> updatedCounts(failedCheck, fixCount, mmsisWithFailedChecks, check)) //
                                    .filter(check -> check.isOk()) //
                                    .map(check -> check.fix()) //
                                    .doOnNext(fix -> persister.persist(fix))
                                    .compose(o -> toLegs(eezLine, eezPolygon, ports, eezWaypoints, o)) //
                                    .filter(x -> includeLeg(x))) //
                            .sorted((a, b) -> compareByMmsiThenLegStartTime(a, b)) //
                            .doOnNext(x -> write(writer, x)) //
                    ) //
                    .doOnTerminate(Checked.a0(() -> persister.close())) //
                    .toBlocking() //
                    .subscribe();
            System.out.println((System.currentTimeMillis() - t) + "ms");
            System.out.println("total fixes=" + fixCount.get());
            System.out.println("num fixes rejected due failed effective speed check=" + failedCheck.get());
            System.out.println("num mmsis with failed effective speed checks=" + mmsisWithFailedChecks.size());

            try (PrintStream p = new PrintStream("target/info.txt")) {
                p.println("total fixes=" + fixCount.get());
                p.println("num fixes rejected due failed effective speed check=" + failedCheck.get());
                p.println("num mmsis with failed effective speed checks=" + mmsisWithFailedChecks.size());
            }
            try (PrintStream p = new PrintStream("target/failures.txt")) {
                p.println("failures mmsi <TAB> number of rejected fixes");
                for (Integer mmsi : mmsisWithFailedChecks.keySet()) {
                    p.println(mmsi + "\t" + mmsisWithFailedChecks.get(mmsi));
                }
            }
            System.out.println("bad item count=" + badItemCount[0]);
            System.out.println("voyages written to " + output);
        }
    }

    private static Observable<String> gunzippedLines(File file) {

        return Observable.using(() -> {
            try {
                return (Reader) new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, //
                r -> Strings.lines(r), r -> {
                    try {
                        r.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    // faster than ZonedDateTime parsing?
    private static final SimpleDateFormat  sdf = create();

    private static SimpleDateFormat create() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    }

    private static final Fix BAD_FIX = new SpecialFix(0, 0, 0, 0);

    private static Fix toFix(String[] items) {
        // converts csv record to fix
        if (items.length != 4) {
            return BAD_FIX;
        }
        if (items[0].length() == 0) {
            return BAD_FIX;
        }
        int mmsi = Integer.parseInt(items[0]);
        if (mmsi == 0) {
            return BAD_FIX;
        }
        long time;
        try {
            time = sdf.parse(items[1]).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        float lat = Float.parseFloat(items[2]);
        float lon = Float.parseFloat(items[3]);
        return new SpecialFix(mmsi, time, lat, lon);
    }

    private static final class SpecialFix implements Fix {

        private final int mmsi;
        private final long time;
        private final float lat;
        private final float lon;

        public SpecialFix(int mmsi, long time, float lat, float lon) {
            this.mmsi = mmsi;
            this.time = time;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public Fix fix() {
            return this;
        }

        @Override
        public float lat() {
            return lat;
        }

        @Override
        public float lon() {
            return lon;
        }

        @Override
        public int mmsi() {
            return mmsi;
        }

        @Override
        public long time() {
            return time;
        }

        @Override
        public Optional<NavigationalStatus> navigationalStatus() {
            return Optional.empty();
        }

        @Override
        public Optional<Float> speedOverGroundKnots() {
            return Optional.empty();
        }

        @Override
        public Optional<Float> courseOverGroundDegrees() {
            return Optional.empty();
        }

        @Override
        public Optional<Float> headingDegrees() {
            return Optional.empty();
        }

        @Override
        public AisClass aisClass() {
            return AisClass.A;
        }

        @Override
        public Optional<Integer> latencySeconds() {
            return Optional.empty();
        }

        @Override
        public Optional<Short> source() {
            return Optional.empty();
        }

        @Override
        public Optional<Byte> rateOfTurn() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Fix [mmsi=" + mmsi + ", time=" + new Date(time) + ", lat=" + lat + ", lon=" + lon + "]";
        }

    }

    public static void main(String[] args) throws Exception {
//        List<File> list = Arrays.asList(new File("/home/dave/cts2017mmsis.csv"));
        List<File> list = Arrays.asList(new File("/home/dave/Downloads/2017.sorted.txt.gz"));
        VoyageDatasetProducer2.produce(new File("target/output.txt"), new File("target/output.tracks"), list);
    }
}
