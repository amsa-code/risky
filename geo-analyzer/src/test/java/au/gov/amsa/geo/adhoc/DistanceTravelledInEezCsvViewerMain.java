package au.gov.amsa.geo.adhoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import au.gov.amsa.ais.rx.Streams;

public class DistanceTravelledInEezCsvViewerMain {

    public static void main(String[] args) throws FileNotFoundException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Set<String> mmsis = new HashSet<>();
        Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
//                .doOnNext(System.out::println)
//                .take(3) //
                .skip(1) //
                .filter(line -> line.trim().length() > 0) //
                .map(x -> x.split(",")) //
                //.filter(x -> x[2].equals("A")) //
                // .reduce(0.0, (sum, x) -> sum + Double.parseDouble(x[4])) //
                .filter(x -> speedKnots(x) > 30) //
                .doOnNext(x -> mmsis.add(x[1])) //
                .map(x -> Arrays.toString(x) + ": avg speed in knots =  " + speedKnots(x)) //
                .doOnNext(System.out::println) //
                .toBlocking() //
                .subscribe();
        System.out.println(mmsis.size());
        System.out.println("finished");
    }

    private static double speedKnots(String[] x) {
        return Double.parseDouble(x[4])/Double.parseDouble(x[5]);
    }

}
