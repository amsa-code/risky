package au.gov.amsa.geo.adhoc;

import java.io.File;

import au.gov.amsa.geo.VoyageDatasetProducer;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.util.identity.MmsiValidator2;
import rx.Observable;

public class DistanceTravelledInEezMain {

    private enum Location {
        IN, OUT, UNKNOWN;
    }

    private static final class State {
        double lat;
        double lon;
        long time;
        Location location;
    }

    public static void main(String[] args) {
        Shapefile eezLine = loadEezLine();
        File tracks = new File("/home/dxm/combinedSortedTracks");
        long t = System.currentTimeMillis();
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
                                        state.lat = fix.lat();
                                        state.lon = fix.lon();
                                        state.location = Location.UNKNOWN;
                                    }).count() //
                                    .map(f -> new Vessel(o.getKey(), f));
                        })) //
                .forEach(x -> System.out.println(x.mmsi + ": " + x.count));
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static Shapefile loadEezLine() {
        return Shapefile.fromZip(DistanceTravelledInEezMain.class.getResourceAsStream("/eez_aust_mainland_line.zip"));
    }

    static final class Vessel {
        final int mmsi;
        final long count;

        Vessel(int mmsi, long count) {
            this.mmsi = mmsi;
            this.count = count;
        }

    }

}
