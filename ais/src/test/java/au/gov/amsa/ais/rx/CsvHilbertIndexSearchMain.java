package au.gov.amsa.ais.rx;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.github.davidmoten.bigsorter.Serializer;
import com.github.davidmoten.shi.Bounds;
import com.github.davidmoten.shi.Index;
import com.github.davidmoten.shi.WithStats;

public class CsvHilbertIndexSearchMain {

    public static void main(String[] args) {
        Index<CSVRecord> index = Index
                .serializer(Serializer.csv(CSVFormat.DEFAULT.withRecordSeparator('\n'),
                        StandardCharsets.UTF_8)) //
                .pointMapper(r -> {
                    try {
                        long time = Long.parseLong(r.get(2));
                        double lat = Double.parseDouble(r.get(3));
                        double lon = Double.parseDouble(r.get(4));
                        return new double[] { lat, lon, time };
                    } catch (Throwable e) {
                        System.out.println(r);
                        throw new RuntimeException(e);
                    }
                }) //
                .read(new File(System.getProperty("user.home")
                        + "/Downloads/2018-11-27-positions-sorted.csv.idx"));
        long t1 = Math.round(index.mins()[2]) + TimeUnit.HOURS.toMillis(12);
        long t2 = t1 + TimeUnit.MINUTES.toMillis(60);
        final Bounds sydney = Bounds.create(new double[] { -33.68, 150.86, t1 },
                new double[] { -34.06, 151.34, t2 });
        final Bounds brisbane = Bounds.create(new double[] { -24.9, 150, t1 },
                new double[] { -29.5, 158, t2 });
        final Bounds qld = Bounds.create(new double[] { -9.481, 137.3, t1 },
                new double[] { -29.0, 155.47, t2 });
        final Bounds tas = Bounds.create(new double[] { -39.389, 143.491, t1 },
                new double[] { -44, 149.5, t2 });
        index.search(sydney).withStats()
                .file(new File(System.getProperty("user.home")
                        + "/Downloads/2018-11-27-positions-sorted.csv"))
                .last().forEach(System.out::println);
        for (Bounds bounds : new Bounds[] { sydney, brisbane, qld, tas }) {
            index.search(bounds).withStats().url(
                    "https://moten-fixes.s3-ap-southeast-2.amazonaws.com/2018-11-27-positions-sorted.csv") //
                    .last().forEach(System.out::println);
        }
    }

}
