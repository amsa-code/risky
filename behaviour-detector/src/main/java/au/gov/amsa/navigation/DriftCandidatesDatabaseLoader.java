package au.gov.amsa.navigation;

import java.util.Date;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import au.gov.amsa.risky.format.Fix;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.jdbc.tuple.Tuple2;

public class DriftCandidatesDatabaseLoader {

    private final Database db;
    private final Map<String, Integer> navStatuses;
    private final int bufferSize;

    public DriftCandidatesDatabaseLoader(Database db, int bufferSize) {
        this.db = db;
        this.bufferSize = bufferSize;
        this.navStatuses = db.select("select name, ais_nav_status_id from aussar.ais_nav_status")
                // to tuple
                .getAs(String.class, Integer.class).toMap(keySelector(), valueSelector())
                .toBlocking().single();
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

    public Transformer<DriftCandidate, Boolean> loadToDatabase() {
        return new Transformer<DriftCandidate, Boolean>() {

            @Override
            public Observable<Boolean> call(Observable<DriftCandidate> candidates) {
                return candidates
                // to row vectors for table insert
                        .flatMap(new Func1<DriftCandidate, Observable<Object>>() {

                            @Override
                            public Observable<Object> call(DriftCandidate c) {
                                Fix f = c.fix();
                                Object navStatus = f.navigationalStatus().isPresent() ? navStatuses
                                        .get(f.navigationalStatus().get().name()) : null;// NullSentinel.create(Types.INTEGER);
                                final Object driftingSince;
                                if (c.driftingSince() == f.time())
                                    driftingSince = null;// NullSentinel.create(Types.TIMESTAMP);
                                else
                                    driftingSince = new Date(c.driftingSince());
                                // return a vector of parameters to be inserted
                                // into the
                                // table
                                return Observable.<Object> just(f.mmsi(), new Date(f.time()), f
                                        .lat(), f.lon(), f.courseOverGroundDegrees().get(), f
                                        .headingDegrees().get(), f.aisClass().name(), navStatus,
                                        driftingSince);
                            }
                        })
                        // push to database in batches
                        .buffer(bufferSize)
                        // insert batch into database
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
                        });
            }
        };
    }

}
