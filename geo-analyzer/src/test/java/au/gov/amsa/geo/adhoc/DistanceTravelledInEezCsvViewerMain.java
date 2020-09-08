package au.gov.amsa.geo.adhoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import au.gov.amsa.ais.rx.Streams;

public class DistanceTravelledInEezCsvViewerMain {

    public static void main(String[] args) throws FileNotFoundException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
                .skip(1) //
                .filter(line -> line.trim().length() > 0) //
                .sorted((x, y) -> x.compareTo(y)) //
                .take(1000) //
                .doOnNext(System.out::println) //
                .toBlocking() //
                .subscribe();
        System.out.println("finished");
    }

}
