package au.gov.amsa.navigation;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.jdbc.tuple.Tuple2;
import com.github.davidmoten.rx.slf4j.Logging;

public class DriftCandidatesLoadToDatabaseMain {

	public static void main(String[] args) {

		final Database db = Database.from("jdbc:oracle:thin:aussar/aussar@devdbs:1521:AUSDEV");

		final Map<String, Integer> map = db
		        .select("select name, ais_nav_status_id from aussar.ais_nav_status")
		        // to tuple
		        .getAs(String.class, Integer.class).toMap(keySelector(), valueSelector())
		        .toBlocking().single();
		System.out.println(map);

		Observable<Object> candidates = DriftCandidates.fromCsv(
		        new File("/home/dxm/drift-candidates.txt")).flatMap(
		        new Func1<DriftCandidate, Observable<Object>>() {

			        @Override
			        public Observable<Object> call(DriftCandidate c) {
				        Fix f = c.fix();
				        Object navStatus = f.navigationalStatus().isPresent() ? map.get(f
				                .navigationalStatus().get().name()) : null;// NullSentinel.create(Types.INTEGER);
				        final Object driftingSince;
				        if (c.driftingSince() == f.time())
					        driftingSince = null;// NullSentinel.create(Types.TIMESTAMP);
				        else
					        driftingSince = new Date(c.driftingSince());
				        return Observable.<Object> just(f.mmsi(), new Date(f.time()), f
				                .lat(), f.lon(), f.courseOverGroundDegrees().get(), f
				                .headingDegrees().get(), f.aisClass().name(), navStatus,
				                driftingSince);
			        }
		        });

		candidates.buffer(9000).lift(Logging.<List<Object>> logger().showCount().log())
		        .concatMap(new Func1<List<Object>, Observable<Boolean>>() {

			        @Override
			        public Observable<Boolean> call(List<Object> candidates) {
				        Observable<Integer> insert = db
				                .update("insert into aussar.drift_candidate(mmsi, time, lat, lon, course, heading, ais_class,ais_nav_status_id, drifting_start_time) "
				                        + "values( ?,?,?,?,?,?,?,?,?) ")
				                // specify params
				                .parameters(Observable.from(candidates))
				                // use a transaction
				                .dependsOn(db.beginTransaction())
				                // count
				                .count()
				                // add counts
				                .reduce(0, Functions.<Integer> add());
				        return db.commit(insert);
			        }
		        })
		        // go
		        .subscribe();

	}

	private static Func1<Tuple2<String, Integer>, String> keySelector() {
		return new Func1<Tuple2<String, Integer>, String>() {

			@Override
			public String call(Tuple2<String, Integer> tuple) {
				return tuple.value1();
			}
		};
	}

	private static Func1<Tuple2<String, Integer>, Integer> valueSelector() {
		return new Func1<Tuple2<String, Integer>, Integer>() {

			@Override
			public Integer call(Tuple2<String, Integer> tuple) {
				return tuple.value2();
			}
		};
	}
}
