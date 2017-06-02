package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import com.github.davidmoten.rx.slf4j.Logging;

import au.gov.amsa.ihs.reader.IhsReader;
import au.gov.amsa.ihs.reader.Key;
import au.gov.amsa.navigation.DriftCandidate;
import au.gov.amsa.navigation.DriftCandidates;
import au.gov.amsa.navigation.ShipStaticData;
import au.gov.amsa.navigation.ShipStaticData.Info;

public class DriftCandidatesMain {

    public static void main(String[] args) throws FileNotFoundException {
        PrintStream out = new PrintStream("target/output.txt");
        out.format("%s\t%s\t%s\t%s\t%s\n", "mmsi", "imo", "date", "lat", "lon");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<Integer, Info> ships = ShipStaticData.getMapFromResource("/ship-data-2014.txt");
        Map<String, Map<String, String>> ihs = IhsReader
                .fromZipAsMapByMmsi(new File("/media/an/ship-data/ihs/608750-2015-04-01.zip"))
                .toBlocking().single();
        for (int year = 2012; year <= 2014; year++) {
            DriftCandidates
                    .fromCsv(new File(
                            "/media/an/drift-candidates/drift-candidates-" + year + ".txt.gz"),
                    true)
                    // log
                    .lift(Logging.<DriftCandidate> logger().showCount().showMemory().every(100000)
                            .log())
                    // grab the first position of a drift track
                    .filter(c -> c.driftingSince() == c.fix().time())
                    // one report per vessel per day
                    .distinct(
                            c -> c.fix().mmsi() + ":"
                                    + DateTimeFormatter.ISO_DATE.format(Instant
                                            .ofEpochMilli(c.fix().time()).atZone(ZoneOffset.UTC)))
                    // print results
                    .doOnNext(c -> {
                        // lookup the imo from ais ship static reports
                        Optional<Info> aisInfo = Optional.ofNullable(ships.get(c.fix().mmsi()));
                        Optional<String> aisImo;
                        Optional<String> ihsImo;
                        if (aisInfo.isPresent() && aisInfo.get().imo.isPresent()) {
                            aisImo = Optional.of(aisInfo.get().imo.get());
                            ihsImo = Optional.empty();
                        } else {
                            aisImo = Optional.empty();
                            // lookup the imo from ihs data
                            Optional<Map<String, String>> ihsInfo = Optional
                                    .ofNullable(ihs.get(c.fix().mmsi()));

                            if (ihsInfo.isPresent()) {
                                ihsImo = Optional
                                        .ofNullable(ihsInfo.get().get(Key.LRIMOShipNo.toString()));
                            } else {
                                ihsImo = Optional.empty();
                            }
                        }
                        String imo = aisImo.orElse(ihsImo.orElse(""));
                        out.format(
                                "%s\t%s\t%s\t%s\t%s\n", c.fix().mmsi(), imo, dtf.format(Instant
                                        .ofEpochMilli(c.fix().time()).atZone(ZoneOffset.UTC)),
                                c.fix().lat(), c.fix().lon());
                    })
                    // go
                    .count().toBlocking().single();
        }
        out.close();
    }

}
