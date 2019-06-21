package au.gov.amsa.ais.rx;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;

import com.github.davidmoten.bigsorter.Serializer;
import com.github.davidmoten.shi.Index;

public class CsvHilbertIndexCreatorMain {

    public static void main(String[] args) {
        File input = new File(System.getProperty("user.home") + "/Downloads/2018-11-27-positions.csv");
        Index //
                .serializer(Serializer.csv(CSVFormat.DEFAULT.withRecordSeparator('\n'), StandardCharsets.UTF_8)) //
                .pointMapper(r -> {
                    long time = Long.parseLong(r.get(2));
                    double lat = Double.parseDouble(r.get(3));
                    double lon = Double.parseDouble(r.get(4));
                    return new double[] { lat, lon, time };
                }) //
                .input(input) //
                .output(new File("target/2018-11-27-positions-sorted.csv")) //
                .bits(10) //
                .dimensions(3) //
                .createIndex("target/2018-11-27-positions-sorted.csv.idx");
    }

}
