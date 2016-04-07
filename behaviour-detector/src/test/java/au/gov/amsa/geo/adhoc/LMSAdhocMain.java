package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.ais.ShipTypeDecoder;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.navigation.ShipStaticData;
import au.gov.amsa.navigation.ShipStaticData.Info;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import rx.Observable;
import rx.functions.Func2;

public class LMSAdhocMain {

    private static final Logger log = LoggerFactory.getLogger(LMSAdhocMain.class);

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Map<Integer, Info> ships = ShipStaticData.getMapFromResource("/ship-data-2014.txt");
        Pattern pattern = Pattern.compile(".*\\.track");
        File base = new File("/media/an/binary-fixes-lms");
        List<File> files = Files.find(new File(base, "2015"), pattern);
        files.addAll(Files.find(new File(base, "2016"), pattern));
        log.info("files=" + files.size());

        Func2<Fix, Fix, Integer> ascendingTime = (a, b) -> ((Long) a.time()).compareTo(b.time());

        String[] shapes = new String[] { "fremantle_port_limits_pl.zip",
                "dampier_port_limits_pl.zip", "port_hedland_port_limits_pl.zip" };
        File shapeBase = new File("/media/an/shapefiles/port-boundaries");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneOffset.UTC);
        System.out.println("region, mmsi, imo, class, type, lat, lon, timeUTC");
        for (String shape : shapes) {
            final String shapeName = shape.replace("_port_limits_pl.zip", "");
            try (InputStream is = new FileInputStream(new File(shapeBase, shape))) {
                Shapefile region = Shapefile.fromZip(is);
                Observable.from(files)
                        // only international vessels
                        .filter(file -> !file.getName().startsWith("503"))
                        //
                        // .filter(file ->
                        // file.getName().startsWith("215040000")
                        // || file.getName().startsWith("351424000"))
                        // .doOnNext(System.out::println)
                        .concatMap(file -> detectCrossings(file, region))
                        // count out to in crossings
                        .toSortedList(ascendingTime).flatMap(o -> Observable.from(o))
                        // log
                        .doOnNext(fix -> {
                            Info info = ships.get(fix.mmsi());
                            String type = "UNKNOWN";
                            if (info != null) {
                                if (info.shipType.isPresent())
                                    type = ShipTypeDecoder.getShipType(info.shipType.get());
                                else
                                    type = "UNKNOWN";
                            }
                            String t = formatter
                                    .format(ZonedDateTime.ofInstant(
                                            Instant.ofEpochMilli(fix.time()), ZoneOffset.UTC))
                                    .replace("[UTC]", "");
                            String imo = info == null ? "" : info.imo.or("");
                            System.out.format("%s,%s,%s,%s,%s,%s,%s,%s\n", shapeName, fix.mmsi(),
                                    imo, fix.aisClass().name(), type, fix.lat(), fix.lon(), t);
                        }).count().toBlocking().single();
            }
        }

    }

    private static final DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC);

    private static Observable<Fix> detectCrossings(File file, Shapefile region) {
        return BinaryFixes.from(file)
                // log
                // .lift(Logging.<Fix> logger().onCompleted("read " +
                // file).every(10000000)
                // .showValue(false).log())
                //
                .filter(fix -> fix.mmsi() != 0)
                // add in or out of region
                .map(fix -> new FixAndRegion(fix, region.contains(fix.lat(), fix.lon())))
                // detect changes in being in
                // our out of region
                .distinctUntilChanged(fix -> fix.inRegion)
                // pair them up
                .buffer(2)
                // just get out of region to
                // inside region
                .filter(list -> list.size() == 2 && !list.get(0).inRegion && list.get(1).inRegion)
                // get the fix inside the region
                .map(list -> list.get(1).fix)
                // one per day
                .distinctUntilChanged(fix -> date.format(
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(fix.time()), ZoneOffset.UTC)));
    }

    private static class FixAndRegion {
        final Fix fix;
        final boolean inRegion;

        FixAndRegion(Fix fix, boolean inRegion) {
            super();
            this.fix = fix;
            this.inRegion = inRegion;
        }

    }

}
