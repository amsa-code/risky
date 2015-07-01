package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func2;
import au.gov.amsa.ais.ShipTypeDecoder;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.navigation.ShipStaticData;
import au.gov.amsa.navigation.ShipStaticData.Info;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;

public class CountCrossingsIntoRegionMain {

    private static final Logger log = LoggerFactory.getLogger(CountCrossingsIntoRegionMain.class);

    public static void main(String[] args) {
        String shape = "/home/dxm/temp/amsa_atba_ningaloo_reef_pl.shp";
        // String shape = "/home/dxm/temp/amb06_map_eez_pl.shp";
        Shapefile region = Shapefile.from(new File(shape));
        // test load
        region.contains(0, 0);

        Func2<Fix, Fix, Integer> ascendingTime = (a, b) -> ((Long) a.time()).compareTo(b.time());
        Map<Long, Info> ships = ShipStaticData.getMapFromResource("/ship-data.txt");
        Pattern pattern = Pattern.compile(".*\\.track");
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2012"), pattern);
        files.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2013"), pattern));
        files.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern));
        files.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2015"), pattern));
        log.info("files=" + files.size());

        int count = Observable
                .from(files)
                // .doOnNext(System.out::println)
                .concatMap(file -> detectCrossings(file, region))
                // count out to in crossings
                .toSortedList(ascendingTime)
                .flatMap(o -> Observable.from(o))
                // log
                .doOnNext(
                        fix -> {
                            Info info = ships.get(fix.mmsi());
                            if (info != null) {
                                String type;
                                if (info.shipType.isPresent())
                                    type = ShipTypeDecoder.getShipType(info.shipType.get());
                                else
                                    type = "UNKNOWN";
                                String t = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime
                                        .ofInstant(Instant.ofEpochMilli(fix.time()),
                                                ZoneId.of("UTC")));
                                System.out.println(t + ", mmsi=" + fix.mmsi() + ", class="
                                        + fix.aisClass().name() + ", lengthMetres="
                                        + info.lengthMetres().orNull() + ", type=" + type
                                        + ", lat=" + fix.lat() + ", lon=" + fix.lon());
                            }
                        }).count().toBlocking().single();
        System.out.println("count=" + count);
    }

    private static Observable<Fix> detectCrossings(File file, Shapefile region) {
        return BinaryFixes.from(file)
        // log
        // .lift(Logging.<Fix>
        // logger().showCount().every(100000).log())
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
                .map(list -> list.get(1).fix);
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
