package au.gov.amsa.navigation;

import java.io.FileNotFoundException;
import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.ais.AisVesselPositions;
import au.gov.amsa.risky.format.HasFix;

public class DriftDetectorMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filename = "/media/analysis/nmea/2013/NMEA_ITU_20130108.gz";
        Streams.nmeaFromGzip(filename)
                .compose(AisVesselPositions.positions())
                .compose(DriftDetector.detectDrift())
                // group by mmsi
                .groupBy(new Func1<HasFix, Long>() {
                    @Override
                    public Long call(HasFix f) {
                        return f.fix().mmsi();
                    }
                })
                .flatMap(
                        new Func1<GroupedObservable<Long, DriftCandidate>, Observable<DriftCandidate>>() {

                            @Override
                            public Observable<DriftCandidate> call(
                                    GroupedObservable<Long, DriftCandidate> positions) {
                                return positions.first();
                            }
                        })
                //
                .doOnNext(new Action1<HasFix>() {

                    @Override
                    public void call(HasFix f) {
                        System.out.println(f);
                    }
                }).subscribe();
    }

}
