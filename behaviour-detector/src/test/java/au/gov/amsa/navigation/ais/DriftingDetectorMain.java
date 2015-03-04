package au.gov.amsa.navigation.ais;

import java.io.FileNotFoundException;
import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.DriftingDetector;
import au.gov.amsa.navigation.VesselPosition;

public class DriftingDetectorMain {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		String filename = "/media/analysis/nmea/2013/NMEA_ITU_20130108.gz";
		Streams.nmeaFromGzip(filename)
		        .compose(AisVesselPositions.positions())
		        .compose(DriftingDetector.detectDrift())
		        .groupBy(new Func1<VesselPosition, Long>() {
			        @Override
			        public Long call(VesselPosition p) {
				        return p.id().uniqueId();
			        }
		        })
		        .flatMap(
		                new Func1<GroupedObservable<Long, VesselPosition>, Observable<VesselPosition>>() {

			                @Override
			                public Observable<VesselPosition> call(
			                        GroupedObservable<Long, VesselPosition> positions) {
				                return positions.first();
			                }
		                }).doOnNext(new Action1<VesselPosition>() {

			        @Override
			        public void call(VesselPosition p) {
				        // System.out.println(p.lat() + "\t" + p.lon() + "\t"
				        // + p.id().uniqueId());
				        System.out.println(p);
			        }
		        }).subscribe();
	}

}
