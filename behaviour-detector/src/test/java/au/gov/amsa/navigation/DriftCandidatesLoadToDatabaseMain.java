package au.gov.amsa.navigation;

import java.io.File;

import com.github.davidmoten.rx.jdbc.Database;
import com.github.davidmoten.rx.slf4j.Logging;

public class DriftCandidatesLoadToDatabaseMain {

    public static void main(String[] args) {

        final Database db = Database.from("jdbc:oracle:thin:aussar/aussar@devdbs:1521:AUSDEV");

        int bufferSize = 1000;
        DriftCandidates
        // load drift candidates from csv
                .fromCsv(new File("/home/dxm/drift-candidates.txt"))
                // log
                .lift(Logging.<DriftCandidate> logger().showCount().every(100).log())
                // write candidates to the database
                .compose(new DriftCandidatesDatabaseLoader(db, bufferSize).loadToDatabase())
                // go
                .subscribe();

    }
}
